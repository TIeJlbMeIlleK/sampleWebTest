package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_03_UseNewDevice extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_03_UseNewDevice";
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);


    //TODO Тест кейс подразумевает уже наполненные справочники ГИС и включенную интеграцию с ГИС

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(2);

        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detach("Коды ответов ВЭС")
                .attachVESCode46("Коды ответов ВЭС")
        .sleep(30);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения","IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:","0")
                .save();
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 2; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию \"Транзакция через госуслуги\" № 1 в интернет-банке",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_INTEGR_VES_NEW);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения","IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:","1")
                .save();

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения","VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:","0")
                .save();
    }

    @Test(
            description = "Провести транзакцию \"Транзакция через госуслуги\" № 2 в интернет-банке",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA_NEW);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения","VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:","10000")
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 3 \"Транзакция через госуслуги\", в интернет-банке",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        String sessionID =  transactionData
                .getSessionId();

        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(1))
                .replaceAll("305",clientIds.get(1))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",sessionID));
        getRabbit()
                .sendMessage();
        getRabbit().close();
        sendAndAssert(transaction);

        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ALERTS);
        commandServiceMock.stop();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
