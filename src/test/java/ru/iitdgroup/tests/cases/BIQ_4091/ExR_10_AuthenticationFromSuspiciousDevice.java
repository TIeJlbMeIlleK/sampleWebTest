package ru.iitdgroup.tests.cases.BIQ_4091;

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

public class ExR_10_AuthenticationFromSuspiciousDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String TABLE= "(System_parameters) Интеграционные параметры";
    private static final String Table_DFP = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";
    private static final String Table_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String Table_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String Table_IMSI = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String DFP = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IFV = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15);
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);



    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(2);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(30);
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceTable() {
        Table.Formula rows1 = getIC().locateTable(Table_DFP).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(Table_DFP)
                .addRecord()
                .fillMasked("DeviceFingerPrint:",DFP)
                .save();
        Table.Formula rows2 = getIC().locateTable(Table_IFV).findRowsBy();
        if (rows2.calcMatchedRows().getTableRowNums().size() > 0) {
            rows2.delete();
        }
        getIC().locateTable(Table_IFV)
                .addRecord()
                .fillMasked("Identifier for vendor:",IFV)
                .save();
        Table.Formula rows3 = getIC().locateTable(Table_IMEI).findRowsBy();
        if (rows3.calcMatchedRows().getTableRowNums().size() > 0) {
            rows3.delete();
        }
        getIC().locateTable(Table_IMEI)
                .addRecord()
                .fillMasked("imei:",IMEI)
                .save();
        Table.Formula rows4 = getIC().locateTable(Table_IMSI).findRowsBy();
        if (rows4.calcMatchedRows().getTableRowNums().size() > 0) {
            rows4.delete();
        }
        getIC().locateTable(Table_IMSI)
                .addRecord()
                .fillMasked("imsi:",IMSI)
                .save();
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "editReferenceTable"
    )
    public void enableVES() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableVES"
    )
    public void client() {
        try {
            for (int i = 0; i < 5; i++) {
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
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV. Отправить транзакцию по данному клиенту",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        commandServiceMock.run();
        Authentication authentication = getAuthentication();
        authentication
                .getData()
                .getClientAuthentication()
                .getClientIds()
                .setDboId(clientIds.get(0));
        authentication
                .getData()
                .getClientAuthentication()
                .getClientDevice()
                .setAndroid(null);
        authentication.getData().getClientAuthentication().getClientDevice().setPlatform(PlatformKind.IOS);
        authentication.getData().getClientAuthentication().getClientDevice().setIOS(new IOSDevice());
        authentication.getData().getClientAuthentication().getClientDevice().getIOS().setOSVersion("8");
        authentication.getData().getClientAuthentication().getClientDevice().getIOS().setIdentifierForVendor(IFV);
        authentication.getData().getClientAuthentication().getClientDevice().getIOS().setModel("10");
        authentication.getData().getClientAuthentication().getClientDevice().getIOS().setIpAddress("192.168.10.1");
        authentication.getData().getClientAuthentication().getClientDevice().getIOS().setAuthByFingerprint(false);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(authentication);

        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getIOS()
                .setOSVersion("9");
        transactionData.getClientDevice()
                .getIOS()
                .setModel("12");
        transactionData.getClientDevice()
                .getIOS()
                .setIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.getClientDevice()
                .getIOS()
                .setIpAddress("192.168.10.1");
        transactionData.getClientDevice()
                .getIOS()
                .setAuthByFingerprint(false);
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }


    @Test(
            description = "Отправить аутентификацию с сессией № 2 для клиента № 2 с подозрительного IMSI Отправить транзакцию по данному клиенту",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication().getClientIds().setDboId(clientIds.get(1));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .setIMSI(IMSI);
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(authentication);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }
    @Test(
            description = "Отправить аутентификацию с сессией № 3 для клиента № 3 с подозрительного IMEI",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication().getClientIds().setDboId(clientIds.get(2));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid().setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid().setIMEI(IMEI);
        sendAndAssert(authentication);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 с подозрительного DFP",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
//        TODO требуется перепроверить после исправления взведения флага при поступлении подозрительного DFP
        Authentication authentication = getAuthentication();
        authentication.getData().getClientAuthentication().getClientIds().setDboId(clientIds.get(3));
        authentication.getData().getClientAuthentication().setSessionId(DFP);
        authentication.getData().getClientAuthentication().setLogin(clientIds.get(3));
        authentication.getData().getClientAuthentication().getClientIds().setLoginHash(clientIds.get(3));
        authentication.getData().getClientAuthentication().getClientIds().setCifId(clientIds.get(3));
        authentication.getData().getClientAuthentication().getClientIds().setEksId(clientIds.get(3));
        sendAndAssert(authentication);
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(3))
                .replaceAll("305",clientIds.get(3))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",DFP));
        getRabbit()
                .sendMessage();
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 не с подозрительного DFP",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication().getClientIds().setDboId(clientIds.get(4));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .setIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .setIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        String IMEI_rep = authentication.getData().getClientAuthentication().getClientDevice()
                .getAndroid().getIMEI();
        String IMSI_rep = authentication.getData().getClientAuthentication().getClientDevice()
                .getAndroid().getIMSI();
        sendAndAssert(authentication);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData.getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_rep);
        transactionData.getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_rep);
        String sessionID = transactionData.getSessionId();
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(4))
                .replaceAll("305",clientIds.get(4))
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Authentication getAuthentication() {
        Authentication authentication = super.getAuthentication("auth/auth1.xml");
        return authentication;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
