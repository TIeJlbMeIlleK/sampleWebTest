package ru.iitdgroup.tests.cases.BIQ_4077;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
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

public class ExR_13_UnprocessedEvent extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_13_UnprocessedEvent";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .editRule(RULE_NAME)
//                .fillInputText("Время в минутах:","60")
//                .fillCheckBox("Active:",true)
//                .save()
//                .getDriver()
//                .findElementByXPath("//*[@id=\"j_id107:0:breadcrumb\"]")
//                .click();
//
//
//        getIC().locateRules()
//                .selectVisible()
//                .editRule("R01_GR_120_ESPP_NewRecipient")
//                .fillCheckBox("Active:",true)
//                .fillInputText("Задержка:","3")
//                .save()
//                .getDriver()
//                .findElementByXPath("//*[@id=\"j_id107:0:breadcrumb\"]")
//                .click();
//
//        getIC().locateRules()
//                .selectVisible()
//                .editRule("R01_GR_20_NewPayee")
//                .fillCheckBox("Active:",true)
//                .save()
//                .sleep(30);
//
//        getIC().locateTable("(System_parameters) Интеграционные параметры")
//                .findRowsBy()
//                .match("Код значения", "Integr_ESPP_CROSSCHANNEL")
//                .click()
//                .edit()
//                .fillInputText("Значение:", "1").save();
//        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
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
            description = "Отправить транзакцию ЕСПП от Клиента №1 для формирования Алерта",
            dependsOnMethods = "client"
    )
    public void sendTransESPP() {
        //TODO требуется реализовать отправку сообщения от ЕСПП
        commandServiceMock.run();
        Transaction transaction = getTransactionESPP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withEksId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

//    @Test(
//            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV. Отправить транзакцию по данному клиенту",
//            dependsOnMethods = "client"
//    )
//    public void transaction1() {
//
//
//    }
//
//
//    @Test(
//            description = "Отправить аутентификацию с сессией № 2 для клиента № 2 с подозрительного IMSI Отправить транзакцию по данному клиенту",
//            dependsOnMethods = "transaction1"
//    )
//    public void transaction2() {
//        Transaction transaction = getTransactionGETTING_CREDIT_Android();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(1));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        sendAndAssert(transaction);
//        try {
//            Thread.sleep(12_000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
//    }
//    @Test(
//            description = "Отправить аутентификацию с сессией № 3 для клиента № 3 с подозрительного IMEI",
//            dependsOnMethods = "transaction2"
//    )
//
//    public void transaction3() {
//        Transaction transaction = getTransactionGETTING_CREDIT_Android();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(2));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        sendAndAssert(transaction);
//        try {
//            Thread.sleep(12_000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
//    }
//
//    @Test(
//            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 с подозрительного DFP",
//            dependsOnMethods = "transaction3"
//    )
//    public void transaction4() {
//        Transaction transaction = getTransactionGETTING_CREDIT_Android();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(3));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        transactionData.getClientDevice()
//                .getAndroid()
//                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
//        sendAndAssert(transaction);
//        try {
//            Thread.sleep(12_000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
//    }
//
//    @Test(
//            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 не с подозрительного DFP",
//            dependsOnMethods = "transaction4"
//    )
//    public void transaction5() {
//        Transaction transaction = getTransactionGETTING_CREDIT_Android();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(4));
//        String sessionID = transactionData.getSessionId();
//        getRabbit().setVesResponse(getRabbit().getVesResponse()
//                .replaceAll("46","46")
//                .replaceAll("ilushka305",clientIds.get(4))
//                .replaceAll("305",clientIds.get(4))
//                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",sessionID));
//        getRabbit()
//                .sendMessage();
//        getRabbit().close();
//        sendAndAssert(transaction);
//        try {
//            Thread.sleep(12_000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
//        commandServiceMock.stop();
//    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT_Android() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionGETTING_CREDIT_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionESPP() {
        Transaction transaction = getTransactionESPP("testCases/Templates/ESPP_transaction.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
