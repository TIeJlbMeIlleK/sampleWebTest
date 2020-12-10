package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
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

public class ExR_09_UseNewMobileDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE= "(System_parameters) Интеграционные параметры";
    private static String tableIMEI_IMSI = "(Rule_tables) Доверенные устройства для клиента";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.DECEMBER, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private String IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String new_IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String new_IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String DFP_FOR_IOC = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String DFP_FOR_ANDROID = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IFV = "b4ab28f4-448f-4684";
    private String new_IFV = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);


    @Test(
            description = "Создаем клиента"
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
            description = "Настройка и включение правила",
            dependsOnMethods = "client"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(2);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(30);
    }

    @Test(
            description = "Настройка и включение VES",
            dependsOnMethods = "enableRules"
    )
    public void enableVES() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Занести в доверенные устройства № 1 IMEI+IMSI и DFP для клиента № 1\n" +
                    "Занести в доверенные устройство № 2 IFV и DFP для клиента № 1",
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

        getIC().locateTable(tableIMEI_IMSI)
                .addRecord()
                .fillMasked("DeviceFingerPrint:",DFP_FOR_IOC)
                .fillMasked("IdentifierForVendor:",IFV)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:",clientIds.get(0))
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "enableIMEI_IMSI"
    )
    public void transaction1() {
        //TODO требуется перепроверить работу ТК после исправления включения правила
        commandServiceMock.run();
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
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
        transactionData
                .setSessionId(DFP_FOR_ANDROID);
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(0))
                .replaceAll("305",clientIds.get(0))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",DFP_FOR_ANDROID));
        getRabbit()
                .sendMessage();
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }

    @Test(
            description = "Провести транзакцию № 2 с устройства № 2 от клиента № 1",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIdentifierForVendor(IFV);
        transactionData
                .setSessionId(DFP_FOR_IOC);
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(0))
                .replaceAll("305",clientIds.get(0))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",DFP_FOR_IOC));
        getRabbit()
                .sendMessage();
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }
    @Test(
            description = "Провести транзакцию № 3 с устройства № 3(Android) от клиента № 1",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
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
                .setIMSI(new_IMSI);
        String sessionID = transactionData.getSessionId();
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(0))
                .replaceAll("305",clientIds.get(0))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",sessionID));
        getRabbit()
                .sendMessage();
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
    }

    @Test(
            description = "Провести транзакцию № 4 с устройства № 4 (IOC) от клиента № 1",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIdentifierForVendor(new_IFV);
        String sessionID = transactionData.getSessionId();
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(0))
                .replaceAll("305",clientIds.get(0))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",sessionID));
        getRabbit()
                .sendMessage();
        getRabbit().close();
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
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
