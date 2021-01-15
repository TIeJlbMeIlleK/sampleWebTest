package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_09_UseNewMobileDevice extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}};

    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Доверенные устройства для клиента";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IFV2 = new RandomString(15).nextString();
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String DFP = new RandomString(15).nextString();
    private static final String DFP2 = new RandomString(15).nextString();
    private static final String LOGIN_1 = new RandomString(5).nextString();
    private static final String LOGIN_2 = new RandomString(5).nextString();
    private static final String LOGIN_HASH1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN_HASH2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                Client client = new Client("testCases/Templates/client.xml");
                if (i == 0) {
                    client.getData().getClientData().getClient().withLogin(LOGIN_1);
                } else {
                    client.getData().getClientData().getClient().withLogin(LOGIN_2);
                }

                if (i == 0) {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH1);
                } else {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH2);
                }
                client.getData()
                        .getClientData()
                        .getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
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
    }

    @Test(
            description = "Включить правило R01_ExR_09_UseNewMobileDevice",
            dependsOnMethods = "addClient"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:", true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Занести в доверенные устройства № 1 Android для клиента № 1 и" +
                    "Занести в доверенные устройства № 2 IOC для клиента № 1",
            dependsOnMethods = "enableRules"
    )

    public void addRecipients() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM1).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("DeviceFingerPrint:", DFP)
                .fillInputText("IMEI:", IMEI)
                .fillInputText("IdentifierForVendor:", IFV)
                .fillInputText("IMSI:", IMSI)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:", clientIds.get(0))
                .save();
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "addRecipients"
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

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("login", LOGIN_1);
            json.put("login_hash", LOGIN_HASH1);
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
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "enableVES"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(IMEI)
                .withIMSI(IMSI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }

    @Test(
            description = "Провести транзакцию № 2 с устройства № 2 от клиента № 1",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .withSessionId(DFP)
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }

    @Test(
            description = "Провести транзакцию № 3 с устройства № 3 от клиента № 1",
            dependsOnMethods = "step2"
    )

    public void step3() {

        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .withSessionId(DFP2)
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
    }

    @Test(
            description = "Провести транзакцию № 3 с устройства № 3 от клиента № 1",
            dependsOnMethods = "step3"
    )

    public void step4() {

        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .withSessionId(DFP)
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionIOS() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
