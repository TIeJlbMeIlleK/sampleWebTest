package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
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
    private static final String IMEI = "863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String IMSI = "250" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String IFV = new RandomString(20).nextString();
    private static final String DFP = new RandomString(30).nextString();

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);
    }

    @Test(
            description = "Добавить в справочник подозрительных IMSI, Добавить в справочник подозрительных IMEI," +
                    "Добавить в справочник подозрительных IFV, Добавить в справочник подозрительных DFP",
            dependsOnMethods = "enableRules"
    )
    public void editTable() {
        getIC().locateTable(TABLE_IMSI)
                .deleteAll()
                .addRecord()
                .fillMasked("imsi:", IMSI)
                .save();
        getIC().locateTable(TABLE_IMEI)
                .deleteAll()
                .deleteAll()
                .addRecord()
                .fillMasked("imei:", IMEI)
                .save();
        getIC().locateTable(TABLE_IFV)
                .deleteAll()
                .addRecord()
                .fillMasked("Identifier for vendor:", IFV)
                .save();
        getIC().locateTable(TABLE_DFP)
                .deleteAll()
                .addRecord()
                .fillMasked("DeviceFingerPrint:", DFP)
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editTable"
    )
    public void step0() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
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
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного IMEI",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(IMEI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 3 с подозрительного IMSI",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с подозрительного IMEI+IMSI",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
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
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI_AMD_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 5 с подозрительного IFV",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("192.158.11.48")
                .withIdentifierForVendor(IFV);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IFV);
    }

    @Test(
            description = "Выполнить транзакцию № 6 с подозрительного DFP",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .setSessionId(DFP);

        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46","46")
                .replaceAll("ilushka305",clientIds.get(1))
                .replaceAll("305",clientIds.get(1))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf",DFP));
        getRabbit()
                .sendMessage();
        getRabbit().close();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_DFP);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
