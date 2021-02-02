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

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_03_UseNewDevice extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_03_UseNewDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Кира", "Брызгина", "Ивановна"}};

    private static String LOGIN = new RandomString(5).nextString();
    private static String LOGIN1 = new RandomString(5).nextString();
    private static String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static String LOGIN_HASH1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String SESSION_ID = new RandomString(10).nextString();
    private static final String SESSION_ID1 = new RandomString(10).nextString();

    //TODO Тест кейс подразумевает уже наполненные справочники ГИС и включенную интеграцию с ГИС

    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9),
                        (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9)};
                Client client = new Client("testCases/Templates/client.xml");
                if (i == 0) {
                    client.getData().getClientData().getClient().withLogin(LOGIN);
                } else {
                    client.getData().getClientData().getClient().withLogin(LOGIN1);
                }

                if (i == 0) {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH);
                } else {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH1);
                }

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withDboId(dboId[i])
                        .withCifId(dboId[i])
                        .withExpertSystemId(dboId[i])
                        .withEksId(dboId[i])
                        .getAlfaIds()
                        .withAlfaId(dboId[i]);

                sendAndAssert(client);
                clientIds.add(dboId[i]);
                System.out.println(dboId[i]);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "46");
            json.put("login", LOGIN);
            json.put("login_hash", LOGIN_HASH);
            json.put("time", "2021-02-02T08:20:35+03:00");
            json.put("session_id", SESSION_ID);
            json.put("device_hash", SESSION_ID);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Настройка и включение правила",
            dependsOnMethods = "addClient"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate();

        getIC().locateRules()
                .openRecord(RULE_NAME)
                .edit()
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Коды ответов ВЭС")
                .attachVESCode46("Коды ответов ВЭС")
                .sleep(10);

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "0")
                .save();
    }

    @Test(
            description = "Провести транзакцию № 1 \"Платеж по QR-коду через СБП\" в интернет-банке с выключенной интеграцией" +
                    "Включить интеграцию с ВЭС и  Установить VES_TIMEOUT в 0 мс",
            dependsOnMethods = "enableRules"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_INTEGR_VES_NEW);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:", "0")
                .save();
    }

    @Test(
            description = "Провести транзакцию \"Платеж по QR-коду через СБП\" № 2 в интернет-банке" +
                    "и после Установить VES_TIMEOUT в 10000 мс",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA_NEW);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:", "300")
                .save();
    }

    @Test(
            description = "Провести транзакцию № 3 \"Платеж по QR-коду через СБП\", в интернет-банке",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {

        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withSessionId(SESSION_ID);
        sendAndAssert(transaction);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ALERTS);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Платеж по QR-коду через СБП\", в мобильном-банке",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withSessionId(SESSION_ID);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ALERTS);
    }

//    @Test(
//            description = "Провести транзакцию № 5 не содержащую в ответе код 46 (например, несуществующий sessionid)",
//            dependsOnMethods = "transaction4"
//    )
//    public void transaction5() {
//        try {
//            String vesResponse1 = getProps().getRabbitUrl();
//            JSONObject js = new JSONObject(vesResponse1);
//            js.put("customer_id", clientIds.get(1));
//            js.put("type_id", "22");
//            js.put("login", LOGIN1);
//            js.put("login_hash", LOGIN_HASH1);
//            js.put("time", "2021-02-02T08:20:35+03:00");
//            js.put("session_id", SESSION_ID1);
//            js.put("device_hash", SESSION_ID1);
//            String newStr1 = js.toString();
//            getRabbit().setVesResponse(newStr1);
//            getRabbit().sendMessage();
//            getRabbit().close();
//        } catch (JSONException e) {
//            throw new IllegalStateException();
//        }
//
//        Transaction transaction = getTransactionIOS();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(1));
//        transactionData
//                .withSessionId(SESSION_ID1);
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
//    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
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
