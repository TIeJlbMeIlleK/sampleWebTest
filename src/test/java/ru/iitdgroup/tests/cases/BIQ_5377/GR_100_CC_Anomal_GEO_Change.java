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
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Instant;

public class GR_100_CC_Anomal_GEO_Change extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_GR_100_CC_Anomal_GEO_Change";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final long UNIT_TIME = Instant.now().getEpochSecond();
    private static final String CARD_HOLDER_NAME = "Место работы";
    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Эльмира", "Пирожкова", "Викторовна"}, {"Олег", "Муркин", "Петрович"}};
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String PAN_ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String CARD_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);


    @Test(
            description = "Включаем правило и настраиваем справочники"
                )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "GisSystem_GIS")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "1.Передать с КАФ новую карточку клиента №1" +
                    "2. Передать из КАФ нефинансовое событие",
            dependsOnMethods = "createClients"
    )
    public void addClientCAF() {
        try {
            String cafClientResponse = getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafClientQueueName())
                    .getCafClientResponse();
            JSONObject json = new JSONObject(cafClientResponse);
            json.put("clientId", clientIds.get(0));
            json.put("cardId", CARD_ID);
            json.put("pan", PAN_ACCOUNT);
            json.put("account", PAN_ACCOUNT);
            String newStr = json.toString();
            getRabbit().setCafClientResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);

            String cafAlertResponse = getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", clientIds.get(0));
            js.put("cardId", CARD_ID);
            js.put("pan", PAN_ACCOUNT);
            json.put("cardholderName", CARD_HOLDER_NAME);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.remove("ipVereq");
            js.put("transactionId", (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5));
            js.put("rrn", new RandomString(10).nextString());
            js.put("terminalIP", "77.51.50.211");
            String newStr1 = js.toString();
            getRabbit().setCafAlertResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Провести транзакцию № 1 для клиента № 1 \"Запрос на выдачу кредита\", " +
                    "такую что расстояние между  координатами События КАФ и транзакции менее 200 км " +
                    "и скорость больше аномальной (150 км/ч) (ip-адрес Владимир (91.225.151.25)) через 1 секунду.",
            dependsOnMethods = "addClientCAF"
    )

    public void transaction1() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("91.225.151.25");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Аномальная смена геопозиции Клиентом");
    }

    @Test(
            description = "Провести транзакцию № 2 для клиента № 1 \"Запрос на выдачу кредита\"," +
                    "такую что расстояние между координатами События КАФ и транзакции более 201 км, " +
                    "но менее 500 км и скорость больше аномальной (400 км/ч) (ip-адрес Нижнего Новгорода (82.208.124.120)) через 1 секунду.",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("82.208.124.120");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Аномальная смена геопозиции Клиентом");
    }

    @Test(
            description = "Провести транзакцию № 3 от клиента № 1 \"Запрос на выдачу кредита\", " +
                    "такую что расстояние между координатами События КАФ и транзакции более 501 км " +
                    "и скорость больше аномальной (800 км/ч) (ip-адрес Новосибирска (5.128.16.120)) через 1 секунду.",
            dependsOnMethods = "transaction1"
    )

    public void transaction3() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("5.128.16.120");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Аномальная смена геопозиции Клиентом");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
