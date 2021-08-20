package ru.iitdgroup.tests.cases.BIQ_4077;

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
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_100_CC_Anomal_GEO_Change extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_100_CC_Anomal_GEO_Change";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final long UNIT_TIME = Instant.now().getEpochSecond();
    private static final String PAN_ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String CARD_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);
    private static final String CARD_HOLDER_NAME = "Место работы";

    //TODO правило не считывает координаты с нефинансового события, поэтому был отправлен Алерт с Раббита

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextInt(999999999) + "";
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
            description = "Передать карточку клиента КАФ",
            dependsOnMethods = "client"
    )
    public void clientCaf() {
        commandServiceMock.run();
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
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Передать из КАФ нефинансовое событие Клиента № 1 " +
                    "(координаты терминала Москвы 55.756655,37.595151 , Координаты VEReq отсутствуют)).",
            dependsOnMethods = "clientCaf"
    )
    public void nonFinTransCAF() {
        try {
            String cafAlertResponse = getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", clientIds.get(0));
            js.put("cardId", CARD_ID);
            js.put("pan", PAN_ACCOUNT);
            js.put("cardholderName", CARD_HOLDER_NAME);
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
        getRabbit().close();
    }

    @Test(
            description = "Провести транзакцию № 1 для клиента № 1 \"Запрос на выдачу кредита\"," +
                    "такую что расстояние между  координатами События КАФ и транзакции менее 200 км и " +
                    "скорость больше аномальной (150 км/ч) (ip-адрес Владимир (91.225.151.25)) через 1 секунду .",
            dependsOnMethods = "nonFinTransCAF"
    )
    public void sendTransaction() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getAndroid()
                .setIpAddress("91.225.151.25");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ANOMAL_GEO_CHANGE);
    }

    @Test(
            description = "Провести транзакцию № 2 для клиента № 1 \"Запрос на выдачу кредита\", " +
                    "такую что расстояние между координатами События КАФ и транзакции более 201 км, " +
                    "но менее 500 км и скорость больше аномальной (400 км/ч) (ip-адрес Нижнего Новгорода (82.208.124.120)) через 1 секунду.",
            dependsOnMethods = "sendTransaction"
    )
    public void sendTransaction2() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getAndroid()
                .setIpAddress("82.208.124.120");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ANOMAL_GEO_CHANGE);
    }

    @Test(
            description = "Провести транзакцию № 3 от клиента № 1 \"Запрос на выдачу кредита\", " +
                    "такую что расстояние между координатами События КАФ и транзакции более 501 км и " +
                    "скорость больше аномальной (800 км/ч) (ip-адрес Новосибирска (5.128.16.120)) через 1 секунду.",
            dependsOnMethods = "sendTransaction2"
    )
    public void sendTransaction3() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getAndroid()
                .setIpAddress("5.128.16.120");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ANOMAL_GEO_CHANGE);
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

}
