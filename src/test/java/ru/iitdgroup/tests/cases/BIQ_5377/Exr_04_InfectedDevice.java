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
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Exr_04_InfectedDevice extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_04_InfectedDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Олеся", "Зимина", "Петровна"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
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
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time))
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
    }

    @Test(
            description = "Провести транзакцию № 1 \"Платеж по QR-коду через СБП\" в интернет-банке с выключенной интеграцией" +
                    "Включить интеграцию с ВЭС и  Установить VES_TIMEOUT в 0 мс",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
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
            description = "Провести транзакцию № 3 \"Платеж по QR-коду через СБП\", в интернет-банке",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "46");
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("time", "2021-06-12T08:20:35+03:00");
            json.put("session_id", SESSION_ID);
            json.put("device_hash", SESSION_ID);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withSessionId(SESSION_ID);
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, "Вход в ДБО");
    }

    @Test(
            description = "Провести транзакцию № 4 \"Платеж по QR-коду через СБП\", в мобильном-банке",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withSessionId(SESSION_ID);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Вход в ДБО");
    }

    @Test(
            description = "Провести транзакцию № 5 не содержащую в ответе код 46 (например, несуществующий sessionid)",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .withSessionId(SESSION_ID1);
        try {
            JSONObject js = new JSONObject(getRabbit().getVesResponse());
            js.put("customer_id", clientIds.get(1));
            js.put("type_id", "22");
            js.put("login", clientIds.get(1));
            js.put("login_hash", clientIds.get(1));
            js.put("time", "2021-06-12T08:20:35+03:00");
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "transaction5"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionIOS() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }
}