package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.ves.mock.VesMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_09_UseNewMobileDevice_3 extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE= "(System_parameters) Интеграционные параметры";
//    private static String tableDFP = "";
    private static String tableIMEI_IMSI = "(Rule_tables) Доверенные устройства для клиента";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private final VesMock vesMock = VesMock.create().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");

    private String IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String new_IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String new_IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String DFP_FOR_IOC = "b4ab28f4-448f-4684-90f6-7953bd605a80";
    private String DFP_FOR_ANDROID = "b4ab28f4-448f-4684-90f6-7953bd604c50";
    private String IFV = "b4ab28f4-448f-4684";
    private String new_IFV = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String new_IFV_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    @Test(
            description = "Создаем клиента"
    )
    public void client() {
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
            description = "Настройка и включение правила",
            dependsOnMethods = "client"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Настройка и включение VES",
            dependsOnMethods = "enableRules"
    )
    public void enableVES() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();

    }

    @Test(
            description = "Добавление в доверенные IMSI+IMEI+DFP для клиента 1",
            dependsOnMethods = "enableVES"
    )
    public void enableIMEI_IMSI() {
        Table.Formula rows = getIC().locateTable(tableIMEI_IMSI).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(tableIMEI_IMSI)
                .addRecord()
                .fillMasked("DeviceFingerPrint:",DFP_FOR_ANDROID)
                .fillMasked("IMEI:",IMEI)
                .fillMasked("IMSI:",IMSI)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:",clientIds.get(0))
                .save();
    }

    @Test(
            description = "Добавление DFP+IFV в доверенные устройства для клиента 1",
            dependsOnMethods = "enableIMEI_IMSI"
    )
    public void enableIFV() {
        getIC().locateTable(tableIMEI_IMSI)
                .addRecord()
                .fillMasked("DeviceFingerPrint:",DFP_FOR_IOC)
                .fillMasked("IdentifierForVendor:",IFV)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:",clientIds.get(0))
                .save();
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "enableIFV"
    )
    public void transaction1() {
        vesMock.run();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI);
//        stepNSessionIdForAndroid = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_ANDROID_DEVICE);

//        vesMock.stop();
//        vesMock.setVesExtendResponse(vesMock
//                .getVesExtendResponse()
//                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\"", "\"fingerprint\": \"123\""));
//        vesMock.run();

    }

    @Test(
            description = "Провести транзакцию № 2 с устройства № 1 от клиента № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI);
//        stepNSessionIdForAndroid = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);

//        vesMock.stop();
//        vesMock.setVesExtendResponse(vesMock
//                .getVesExtendResponse()
//                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\"", "\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd605a80\""));
//        vesMock.run();


    }
    @Test(
            description = "Провести транзакцию № 3 с устройства № 2 от клиента № 1",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(IFV);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
//        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IFV);
    }

    @Test(
            description = "Провести транзакцию № 4 с устройства № 2 от клиента № 2",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(IFV);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
//        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }


    @Test(
            description = "Изменить в устройстве № 1 IMEI (устройство № 3). Провести транзакцию № 5 с устройства № 3 для клиента № 1",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(new_IMEI);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI);
//        stepNSessionIdForAndroid = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, REPLACE_IMEI);

//        vesMock.stop();
//        vesMock.setVesExtendResponse(vesMock
//                .getVesExtendResponse()
//                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\"", "\"fingerprint\": \"123\""));
//        vesMock.run();

    }
    @Test(
            description = "Изменить в устройстве № 1 IMSI (устройство № 4). Провести транзакцию № 6 с устройства № 4 для клиента № 1",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(new_IMSI);
//        stepNSessionIdForAndroid = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, REPLACE_SIM);



    }

    @Test(
            description = "Изменить в устройстве № 2 IFV (устройство № 5) Провести транзакцию № 7 с устройства № 5 для клиента № 1",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", false)
                .save()
                .sleep(5);
        getIC().close();
        vesMock.stop();
        vesMock.setVesExtendResponse(vesMock
                .getVesExtendResponse()
                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd604c50\"", "\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd605a80\""));
        vesMock.run();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(new_IFV);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
//        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
    }

    @Test(
            description = " Изменить в устройстве № 2 DFP (устройство № 6) Провести транзакцию № 8 с устройства № 6 для клиента № 1",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        vesMock.stop();
        vesMock.setVesExtendResponse(vesMock
                .getVesExtendResponse()
                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-4684-90f6-7953bd605a80\"", "\"fingerprint\": \"b4ab28f4-448f-1sd5-90f6-7s47bd604aaa\""));
        vesMock.run();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("9");
        transactionData.getClientDevice().getIOS().setModel("12");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(IFV);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
//        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(
            description = " Изменить в устройстве № 2 IFV и DFP (устройство № 7) Провести транзакцию № 9 с устройства № 7 для клиента № 1",
            dependsOnMethods = "transaction8"
    )
    public void transaction9() {
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
        vesMock.stop();
        vesMock.setVesExtendResponse(vesMock
                .getVesExtendResponse()
                .replaceAll("\"fingerprint\": \"b4ab28f4-448f-1sd5-90f6-7s47bd604aaa\"","\"fingerprint\": \"b4ab28f4-448f-4684-57re51-7953bd615fggh7fg\""));
        vesMock.run();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("9");
        transactionData.getClientDevice().getIOS().setModel("12");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(new_IFV_2);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
//        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
    }


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
