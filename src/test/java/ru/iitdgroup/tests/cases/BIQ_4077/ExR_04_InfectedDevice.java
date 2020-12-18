package ru.iitdgroup.tests.cases.BIQ_4077;

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

public class ExR_04_InfectedDevice extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_04_InfectedDevice";
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
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию \"Запрос на выдачу кредита\" № 1 в интернет-банке",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionGETTING_CREDIT();
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
            description = "Провести транзакцию \"Запрос на выдачу кредита\" № 2 в интернет-банке",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);

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
            description = "Провести транзакцию № 3 \"Запрос на выдачу кредита\", в интернет-банке",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionGETTING_CREDIT();
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
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ALERT_FROM_VES);
        commandServiceMock.stop();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
