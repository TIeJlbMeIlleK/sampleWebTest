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


public class GR_105_CC_UnprocessedCafEventSender extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_GR_105_CC_UnprocessedCafEventSender";

    private static final long UNIT_TIME = Instant.now().getEpochSecond();//конвертирует текущее время в UNIT TIME

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Эльмира", "Пирожкова", "Викторовна"}};
    private static String[] login = {new RandomString(5).nextString(), new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static final String PAN_ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String CARD_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);
    private static final String CARD_HOLDER_NAME = "Место работы";



    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 2; i++) {
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withLogin(login[i])
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(loginHash[i])
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
    }

    @Test(
            description = "1.Передать из КАФ Событие КАФ для Клиента № 1. Статус События \"Не обработано\".",
            dependsOnMethods = "addClients"
    )
    public void addClientCAF() {
        try {
            String cafClientResponse = getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafClientQueueName())
                    .getCafClientResponse();
            JSONObject json = new JSONObject(cafClientResponse);
            json.put("clientId", clientIds.get(0));
            json.put("cardholderName", CARD_HOLDER_NAME);
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
            description = "Провести транзакцию № 1 Клиента № 1 \"Запрос на выдачу кредита\"",
            dependsOnMethods = "addClientCAF"
    )

    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Существует Событие из КАФ у Клиента");
    }

    @Test(
            description = "Провести транзакцию № 1 Клиента № 1 \"Запрос на выдачу кредита\"",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не сработало");
    }

    @Test(
            description = "Перевести Событие КАФ в статус \"Обработано\"" +
                    "И Провести транзакцию № 3 Клиента № 1 \"Запрос на выдачу кредита\"",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        getIC()
                .locateReports()
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN_ACCOUNT)
                .runReport()
                .openFirstID()
                .getActions()
                .doAction("Обработано")
                .approved();
        getIC().close();


        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не сработало");
    }

    @Test(
            description = "Провести транзакцию № 4 Клиента № 2 \"Запрос на выдачу кредита\"",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не сработало");
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
