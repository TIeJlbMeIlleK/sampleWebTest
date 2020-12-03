package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_05_GrayIP extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_05_GrayIP";
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
                .sleep(30);


        Table.Formula rows = getIC().locateTable("(Rule_tables) Подозрительные IP адреса").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable("(Rule_tables) Подозрительные IP адреса")
                .addRecord()
                .fillInputText("Маска подсети устройства:","10.152.150.0/24")
                .fillInputText("IP устройства:","10.152.150.1")
                .save();
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "Выполнить регулярную транзакцию № 1с подозрительного IP-адреса",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.152.150.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного IP-адреса",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.152.150.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IP);
    }

    @Test(
            description = "Выполнить транзакцию № 3 не с подозрительного IP-адреса",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.130.171.5");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_NO_GREY_IP);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с IP-адреса подозрительной маски",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.152.150.10");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IP);
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
