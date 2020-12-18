package ru.iitdgroup.tests.cases.BIQ_4077;

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

public class ExR_06_GrayDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_06_GrayDevice";
    private static final String TABLE_IMSI = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String TABLE_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String TABLE_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String TABLE_DFP = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);



    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 4, 0, 0, 0);
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
            description = "Добавить в справочник подозрительных IMSI",
            dependsOnMethods = "enableRules"
    )
    public void editTableIMSI() {
        Table.Formula rows = getIC().locateTable(TABLE_IMSI).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IMSI)
                .addRecord()
                .fillMasked("imsi:", "250015038779300")
                .save();
    }
    @Test(
            description = "Добавить в справочник подозрительных IMEI",
            dependsOnMethods = "editTableIMSI"
    )
    public void editTableIMEI() {
        Table.Formula rows = getIC().locateTable(TABLE_IMEI).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IMEI)
                .addRecord()
                .fillMasked("imei:", "863313032529520")
                .save();
    }
    @Test(
            description = "Добавить в справочник подозрительных IFV",
            dependsOnMethods = "editTableIMEI"
    )
    public void editTableIFV() {
        Table.Formula rows = getIC().locateTable(TABLE_IFV).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IFV)
                .addRecord()
                .fillMasked("Identifier for vendor:", "3213-5F97-4B54-9A98-748B1CF8AB8C")
                .save();
    }

    @Test(
            description = "Добавить в справочник подозрительных DFP",
            dependsOnMethods = "editTableIFV"
    )
    public void editTableDFP() {
        Table.Formula rows = getIC().locateTable(TABLE_DFP).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_DFP)
                .addRecord()
                .fillMasked("DeviceFingerPrint:", "dfsngfljknssdfg1605sd7fg56d1f")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editTableDFP"
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
            description = "Выполнить регулярную транзакцию № 1 с подозрительного устройства (IMEI/IMSI/IFV)",
            dependsOnMethods = "step0"
    )
    public void step1() {
        commandServiceMock.run();
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного IMEI",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 3 с подозрительного IMSI",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("250015038779300");
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с подозрительного IMEI+IMSI",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("250015038779300");
        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI_AMD_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 5 с подозрительного IFV",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionGETTING_CREDIT_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice()
                .setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().getIOS().setIpAddress("192.158.11.48");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor("3213-5F97-4B54-9A98-748B1CF8AB8C");
        transactionData.getClientDevice().getIOS().setOSVersion("9.1");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);

        sendAndAssert(transaction);
        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IFV);
    }

    @Test(
            description = "Выполнить транзакцию № 6 с подозрительного DFP",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .setSessionId("dfsngfljknssdfg1605sd7fg56d1f");

        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(1))
                .replaceAll("305",clientIds.get(1))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf","dfsngfljknssdfg1605sd7fg56d1f"));
        getRabbit()
                .sendMessage();
        getRabbit().close();
        sendAndAssert(transaction);

        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_DFP);
        commandServiceMock.stop();
    }

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

    private Transaction getTransactionGETTING_CREDIT() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_PC.xml");
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

}
