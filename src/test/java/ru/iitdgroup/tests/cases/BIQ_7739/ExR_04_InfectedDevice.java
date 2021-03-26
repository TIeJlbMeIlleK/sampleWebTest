package ru.iitdgroup.tests.cases.BIQ_7739;

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

public class ExR_04_InfectedDevice extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_04_InfectedDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Олеся", "Зимина", "Петровна"}};

    private static String LOGIN;
    private static String LOGIN1;
    private static String LOGIN_HASH;
    private static String LOGIN_HASH1;
    private static final String SESSION_ID = new RandomString(10).nextString();
    private static final String SESSION_ID1 = new RandomString(10).nextString();

    //TODO Тест кейс подразумевает уже наполненные справочники ГИС и включенную интеграцию с ГИС

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
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
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "VES_LIVE_TIME")
                .click()
                .edit()
                .fillInputText("Значение:", "1440")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                String login = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                String loginHash = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");
                if (i == 0) {
                    LOGIN = login;
                    LOGIN_HASH = loginHash;
                } else {
                    LOGIN1 = login;
                    LOGIN_HASH1 = loginHash;
                }
                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(login)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(loginHash)
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
            description = "Провести транзакцию № 1 \"Заявка на выпуск карты\" в интернет-банке с выключенной интеграцией" +
                    "Включить интеграцию с ВЭС и  Установить VES_TIMEOUT в 0 мс",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        Transaction transaction = getTransactionPC();
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
            description = "Провести транзакцию \"Заявка на выпуск карты\" № 2 в интернет-банке" +
                    "и после Установить VES_TIMEOUT в 10000 мс",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionPC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Недостаточно данных");

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:", "300")
                .save();
    }

    @Test(
            description = "Провести транзакцию № 3 \"Заявка на выпуск карты\", в интернет-банке",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
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

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        Transaction transaction = getTransactionPC();
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
        assertLastTransactionRuleApply(TRIGGERED, "В ответе от ВЭС присутствуют признаки заражения или удаленного управления устройтвом клиента");
    }

    @Test(
            description = "Провести транзакцию № 4 \"Заявка на выпуск карты\", в мобильном-банке",
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
        assertLastTransactionRuleApply(TRIGGERED, "В ответе от ВЭС присутствуют признаки заражения или удаленного управления устройтвом клиента");
    }

    @Test(
            description = "Провести транзакцию № 5 не содержащую в ответе код 46 (например, несуществующий sessionid)",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .withSessionId(SESSION_ID1);
        try {
            JSONObject js = new JSONObject(getRabbit().getVesResponse());
            js.put("customer_id", clientIds.get(1));
            js.put("type_id", "22");
            js.put("login", LOGIN1);
            js.put("login_hash", LOGIN_HASH1);
            js.put("time", "2021-02-02T08:20:35+03:00");
            js.put("session_id", SESSION_ID1);
            js.put("device_hash", SESSION_ID1);
            String vesMessage = js.toString();
            getRabbit().setVesResponse(vesMessage);
            getRabbit().sendMessage();
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
        sendAndAssert(transaction);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
