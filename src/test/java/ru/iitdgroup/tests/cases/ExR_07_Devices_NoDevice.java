package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PCDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.ChannelType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.ves.mock.VesMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_07_Devices_NoDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_07_Devices";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 4, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    private final VesMock vesMock = new VesMock().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        vesMock.run();

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);



    }

    @Test(
            description = "Включить интеграцию с VES",
            dependsOnMethods = "enableRules"
    )

    public void editVES(){
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по суждения . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();

        getIC().close();
    }
//    @Test(
//            description = "Установить значение ожидания ответа от ВЭС меньше значения времени отправки данного ответа",
//            dependsOnMethods = "enableRules"
//    )
//    public void enableVesTimeout() {
//
//        getIC().locateTable("(System_parameters) Интеграционные параметры")
//                .findRowsBy()
//                .match("Description", "Время ожидания актуальных данных от ВЭС")
//                .click()
//                .edit()
//                .fillInputText("Значение:", "1").save();
//
//        getIC().close();
//    }
    @Test(
            description = "Сгенерировать клиентов",
            dependsOnMethods = "editVES"
    )

    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "Провести транзакцию № 1, регулярную",
            dependsOnMethods = "client"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION_1);
    }

    @Test(
            description = "Провести транзакцию № 2, в транзакции отсутствует контейнер Device",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .setClientDevice(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MISSING_DEVICE);
    }
    @Test(
            description = "Провести транзакцию № 3, в транзакции отсутствует IMEI",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(null);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("1567156156741");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_IMEI);
    }

    @Test(
            description = "Провести транзакцию № 4, в транзакции отсутствует IMSI",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("156748541521fd1g165721dfg7");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_IMSI);
    }

    @Test(
            description = "Провести транзакцию № 5, в транзакции отсутствует IFV",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice()
                .getIOS()
                .setAuthByFingerprint(false);
        transactionData.getClientDevice()
                .getIOS()
                .setIpAddress("152.11.54.1");
        transactionData.getClientDevice()
                .getIOS()
                .setIdentifierForVendor(null);
        transactionData.getClientDevice()
                .getIOS()
                .setModel("10");
        transactionData.getClientDevice()
                .getIOS()
                .setOSVersion("8");
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_IFV);
    }

    @Test(
            description = "Выключить интеграцию с ВЭС, провести транзакцию № 6",
            dependsOnMethods = "step5"
    )
    public void step6() {
        vesMock.stop();


        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPC(new PCDevice());
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("123.22.57.8");
        transactionData.getClientDevice()
                .getPC()
                .setBrowserData("45");
        transactionData.getClientDevice()
                .getPC()
                .setUserAgent("415");
        transactionData.setChannel(ChannelType.INTERNET_CLIENT);
        transactionData.getClientDevice().setPlatform(PlatformKind.PC);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_INTEGR_VES);



    }

//    @Test(
//            description = "Включить интеграцию с ВЭС, Выполнить транзакцию № 7 с несуществующего в ВЭС sessionid (DFP не должно поступить в САФ от ВЭС)",
//            dependsOnMethods = "step6"
//    )
//    public void step7() {
//        getIC().locateTable("(System_parameters) Интеграционные параметры")
//                .findRowsBy()
//                .match("Description", "Интеграция с ВЭС по суждения . Если параметр включен – интеграция производится.")
//                .click()
//                .edit()
//                .fillInputText("Значение:", "1").save();
//        getIC().locateTable("(System_parameters) Интеграционные параметры")
//                .findRowsBy()
//                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
//                .click()
//                .edit()
//                .fillInputText("Значение:", "1").save();
//
//        getIC().close();
//
//        Transaction transaction = getTransaction();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(3));
//        transactionData.getClientDevice().setAndroid(null);
//        transactionData.getClientDevice().setPC(new PCDevice());
//        transactionData.getClientDevice()
//                .setPlatform(PlatformKind.PC);
//        transactionData.getClientDevice()
//                .getPC()
//                .setIpAddress("192.115.86.15");
//        transactionData.getClientDevice()
//                .getPC()
//                .setUserAgent("123657");
//        transactionData.getClientDevice()
//                .getPC()
//                .setBrowserData("123456");
//        transactionData.setChannel(ChannelType.INTERNET_CLIENT);
//        transactionData.setSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
//
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_FEW_DATA);
//    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
