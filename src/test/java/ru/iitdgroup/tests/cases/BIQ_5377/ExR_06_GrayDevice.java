package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_06_GrayDevice extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String RULE_NAME = "R01_ExR_06_GrayDevice";
    private static final String REFERENCE_ITEM_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String REFERENCE_ITEM_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String REFERENCE_ITEM_IMSI = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String REFERENCE_ITEM_DFP = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String DFP = new RandomString(15).nextString();

    @Test(
            description = "Включить правило R01_ExR_06_GrayDevice"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM_IFV)
                .deleteAll()
                .addRecord()
                .fillInputText("Identifier for vendor:", IFV)
                .save();

        getIC().locateTable(REFERENCE_ITEM_IMSI)
                .deleteAll()
                .addRecord()
                .fillInputText("imsi:", IMSI)
                .save();

        getIC().locateTable(REFERENCE_ITEM_IMEI)
                .deleteAll()
                .addRecord()
                .fillInputText("imei:", IMEI)
                .save();

        getIC().locateTable(REFERENCE_ITEM_DFP)
                .deleteAll()
                .addRecord()
                .fillInputText("DeviceFingerPrint:", DFP)
                .save();

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().close();

        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("session_id", DFP);
            json.put("device_hash", DFP);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Выполнить регулярную транзакцию № 1 с подозрительного устройства (IMEI/IMSI/IFV)",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(IMEI)
                .withIMSI(IMSI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного устройства IMEI",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(IMEI)
                .withIMSI(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "IMEI найден в сером списке");
    }

    @Test(
            description = "Выполнить транзакцию № 3 с подозрительного устройства IMSI",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(null)
                .withIMSI(IMSI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "IMSI найден в сером списке");
    }

    @Test(
            description = "Выполнить транзакцию № 4 с подозрительного устройства DFP",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withSessionId(DFP)
                .getClientDevice()
                .getAndroid()
                .withIMEI(null)
                .withIMSI(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "DFP найден в сером списке");
    }

    @Test(
            description = "Выполнить транзакцию № 5 с подозрительного устройства IFV",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "IFV найден в сером списке");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step5"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        return transaction;
    }

    private Transaction getTransactionIOS() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        return transaction;
    }
}
