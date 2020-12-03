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

public class ExR_02_AnomalGeoPosChange extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_02_AnomalGeoPosChange";
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
        getIC().close();
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
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1 с ip-адреса Москвы",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REQUIRE_PREVIOUS_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 2 с ip-адреса Токио спустя минуту",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("140.227.60.114");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HISPEED_800);
    }

    @Test(
            description = "Провести транзакцию № 3 с ip-адреса Москвы через 24 часа",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.HOUR, 24);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 4 с ip-адреса Нижнего Новгорода через 1 секунду",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("109.184.14.163");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HISPEED_400);
    }

    @Test(
            description = "Провести транзакцию № 5 с ip-адреса Москвы от нового клиента",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REQUIRE_PREVIOUS_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 6 с ip-адреса Егорьевск через 1 секунду",
            dependsOnMethods = "step5"
    )
    public void step6() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("82.194.247.205");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HISPEED_150);
        commandServiceMock.stop();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
