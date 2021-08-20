package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_09_UseNewMobileDevice extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}};
    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Доверенные устройства для клиента";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IFV2 = new RandomString(15).nextString();
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String DFP = new RandomString(15).nextString();
    private static final String DFP2 = new RandomString(15).nextString();

    @Test(
            description = "Включить правило R01_ExR_09_UseNewMobileDevice"
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
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента" +
                    "Занести в доверенные устройства № 1 Android для клиента № 1 и" +
                    "Занести в доверенные устройства № 2 IOC для клиента № 1",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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

        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("DeviceFingerPrint:", DFP)
                .fillInputText("IMEI:", IMEI)
                .fillInputText("IdentifierForVendor:", IFV)
                .fillInputText("IMSI:", IMSI)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:", clientIds.get(0))
                .save();

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
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "addClient"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .withSessionId(DFP)
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step4"
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
