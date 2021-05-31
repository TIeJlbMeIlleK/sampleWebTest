package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
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
    private static final String tableIMEI_IMSI = "(Rule_tables) Доверенные устройства для клиента";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String new_IMEI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String new_IMSI = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String DFP_FOR_IOC = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String DFP_FOR_ANDROID = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String IFV = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private final String new_IFV = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(20);

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
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
            description = "Занести в доверенные устройства № 1 IMEI+IMSI и DFP для клиента № 1\n" +
                    "Занести в доверенные устройство № 2 IFV и DFP для клиента № 1",
            dependsOnMethods = "client"
    )
    public void enableIMEI_IMSI() {
        getIC().locateTable(tableIMEI_IMSI)
                .deleteAll()
                .addRecord()
                .fillMasked("DeviceFingerPrint:", DFP_FOR_ANDROID)
                .fillMasked("IMEI:",IMEI)
                .fillMasked("IMSI:",IMSI)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:",clientIds.get(0))
                .save();

        getIC().locateTable(tableIMEI_IMSI)
                .addRecord()
                .fillMasked("DeviceFingerPrint:", DFP_FOR_IOC)
                .fillMasked("IdentifierForVendor:",IFV)
                .fillCheckBox("Доверенный:", true)
                .fillUser("Клиент:",clientIds.get(0))
                .save();
        getIC().close();

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "46");
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("time", "2021-05-02T08:20:35+03:00");
            json.put("session_id", DFP_FOR_ANDROID);
            json.put("device_hash", DFP_FOR_ANDROID);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "enableIMEI_IMSI"
    )
    public void transaction1() {

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
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMSI(IMSI);
        transactionData
                .withSessionId(DFP_FOR_ANDROID);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }

    @Test(
            description = "Провести транзакцию № 2 с устройства № 2 от клиента № 1",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "46");
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("time", "2021-05-02T08:20:35+03:00");
            json.put("session_id", DFP_FOR_IOC);
            json.put("device_hash", DFP_FOR_IOC);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_IOC();
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
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_DEVICE_MSG);
    }
    @Test(
            description = "Провести транзакцию № 3 с устройства № 3(Android) от клиента № 1",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
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
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
    }

    @Test(
            description = "Провести транзакцию № 4 с устройства № 4 (IOC) от клиента № 1",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_IOC();
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
        assertLastTransactionRuleApply(TRIGGERED, NEW_DEVICE);
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

    private Transaction getTransactionREQUEST_CARD_ISSUE_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
