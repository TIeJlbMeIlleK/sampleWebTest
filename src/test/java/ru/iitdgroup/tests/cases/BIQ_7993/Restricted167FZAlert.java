package ru.iitdgroup.tests.cases.BIQ_7993;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;
import ru.iitdgroup.tests.webdriver.report.ReportRecord;

import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.testng.AssertJUnit.assertEquals;

public class Restricted167FZAlert extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String serviceName = "Мегафон по номеру телефона";
    private final String providerName = "Мегафон";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Юрий", "Глызин", "Андреевич"}, {"Олег", "Тырин", "Семенович"}, {"Максим", "Туров", "Олегович"}};

    private static final String CARD_HOLDER_NAME = "Глызин Юрий Андреевич";
    private static final String CARD_HOLDER_NAME2 = "Тырин Олег Семенович";

    private static final String ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String ACCOUNT2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

    private static final String PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String PHONE2 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);

    private final static String[] PAN = new String[8];
    private final static String PAN1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String[] CARD_ID = {
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4)};

    private static final String[] CARD_STATUS = {
            "Restricted 167-FZ",
            "Open",
            "Compromised Antifraud",
            "Stolen (voice)",
            "Lost (voice)",
            "K"};

    //TODO перед запуском должен быть создан Action для Алерт WF:
    // -- Display name: Разблокировать все карты 167-ФЗ
    // -- Unique name: massunblock
    // -- В ExternalApi выбрать "(CCAF) Send Card command"
    // Статусная доступность из Any State в Keep Current State

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Оплата услуг")
                .select("Наименование канала:", "Мобильный банк")
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 3; i++) {
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
    }


    @Test(
            description = "Создаем клиентов КАФ",
            dependsOnMethods = "addClients"
    )
    public void addClientsCAF() {
        try {
            for (int i = 0; i < 8; i++) {
                PAN[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

                String cafClientResponse = getRabbit()// создание клиента КАФ с разными статусами карт
                        .getAllQueues()
                        .getQueue(getProps().getRabbitCafClientQueueName())
                        .getCafClientResponse();
                JSONObject json = new JSONObject(cafClientResponse);

                json.put("cardId", CARD_ID[i]);
                json.put("pan", PAN[i]);

                if (i <= 5) {
                    json.put("clientId", clientIds.get(0));
                    json.put("cardholderName", CARD_HOLDER_NAME);
                    json.put("account", ACCOUNT);
                    json.put("3DsecPhone", PHONE);
                    json.put("cardStatus", CARD_STATUS[i]);
                    System.out.println("ID клиента № 1= " + clientIds.get(0));
                    System.out.println("Аккаунт клиента № 1 = " + ACCOUNT);
                } else if (i == 6) {
                    json.put("clientId", clientIds.get(1));
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[1]);
                    System.out.println("ID клиента № 2 = " + clientIds.get(2));
                    System.out.println("Аккаунт клиента № 2 = " + ACCOUNT2);
                } else {
                    json.put("clientId", clientIds.get(1));
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[5]);
                }

                System.out.println("ID карты №" + i + " = " + CARD_ID[i]);
                System.out.println("Номер карты №" + i + " = " + PAN[i]);
                System.out.println("--------------------------------");
                String newStr = json.toString();
                getRabbit().setCafClientResponse(newStr);
                getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);
            }
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
        getRabbit().close();
    }

    @Test(
            description = "1. Отправить транзакции \"Оплата услуг\" от Клиента №1:" +
                    "-- Транзакция №1: SourceCardNumber = Номер карты из предусловия №1" +
                    "-- Транзакция №2: SourceCardNumber = Номер карты из предусловия №2" +
                    "-- Транзакция №3: SourceCardNumber = Номер карты из предусловия №3" +
                    "-- Транзакция №4: SourceCardNumber = Номер карты из предусловия №4" +
                    "-- Транзакция №5: SourceCardNumber = Номер карты из предусловия №5" +
                    "-- Транзакция №6: SourceCardNumber = Номер карты из предусловия №6" +
                    "2. Из каждого Алерта по транзакциям выполнить Action из предусловия" +
                    "3. Проверить отправку сообщений в КАФ по каждой карте на разблокировку",
            dependsOnMethods = "addClientsCAF"
    )
    public void clientsCAF1() {

        time.add(Calendar.MINUTE, -20);
        Transaction transService = getServicePayment();
        TransactionDataType transactionDataService = transService.getData().getTransactionData();
        transactionDataService
                .getServicePayment()
                .withSourceCardNumber(PAN[0]);
        sendAndAssert(transService);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceTwo = getServicePayment();
        TransactionDataType transactionDataServiceTwo = transServiceTwo.getData().getTransactionData();
        transactionDataServiceTwo
                .getServicePayment()
                .withSourceCardNumber(PAN[1]);
        sendAndAssert(transServiceTwo);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceThird = getServicePayment();
        TransactionDataType transactionDataServiceThird = transServiceThird.getData().getTransactionData();
        transactionDataServiceThird
                .getServicePayment()
                .withSourceCardNumber(PAN[2]);
        sendAndAssert(transServiceThird);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceFourth = getServicePayment();
        TransactionDataType transactionDataServiceFourth = transServiceFourth.getData().getTransactionData();
        transactionDataServiceFourth
                .getServicePayment()
                .withSourceCardNumber(PAN[3]);
        sendAndAssert(transServiceFourth);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceFifth = getServicePayment();
        TransactionDataType transactionDataServiceFifth = transServiceFifth.getData().getTransactionData();
        transactionDataServiceFifth
                .getServicePayment()
                .withSourceCardNumber(PAN[4]);
        sendAndAssert(transServiceFifth);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceSixth = getServicePayment();
        TransactionDataType transactionDataServiceSixth = transServiceSixth.getData().getTransactionData();
        transactionDataServiceSixth
                .getServicePayment()
                .withSourceCardNumber(PAN[5]);
        sendAndAssert(transServiceSixth);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[5]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
        assertEquals(res.getFildsValuesLog(12)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(12)[1], "KAF");
    }

    @Test(
            description = "4. Отправить от Клиетна №2 транзакцию \"Оплата услуг\" с SourceCardNumber из предусловия" +
                    "5. Перейти в Алерт по тразнакции от Клиента №2 и выполнить Action из предусловия",
            dependsOnMethods = "clientsCAF1"
    )
    public void clientCAF2() {
        time.add(Calendar.SECOND, 20);
        Transaction transService = getServicePayment();
        TransactionDataType transactionDataService = transService.getData().getTransactionData();
        transactionDataService
                .getClientIds().withDboId(clientIds.get(1));
        transactionDataService
                .getServicePayment()
                .withSourceCardNumber(PAN[6]);
        sendAndAssert(transService);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceTwo = getServicePayment();
        TransactionDataType transactionDataServiceTwo = transServiceTwo.getData().getTransactionData();
        transactionDataServiceTwo
                .getClientIds().withDboId(clientIds.get(1));
        transactionDataServiceTwo
                .getServicePayment()
                .withSourceCardNumber(PAN[7]);
        sendAndAssert(transServiceTwo);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[7]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
        assertEquals(res.getFildsValuesLog(6)[3], CARD_ID[5]);
        assertEquals(res.getFildsValuesLog(6)[1], "KAF");
        assertEquals(res.getFildsValuesLog(17)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(17)[1], "KAF");
    }

    @Test(
            description = "6. Отправить от Клиента №3 транзакцию \"Оплата услуг\" с любым SourceCardNumber" +
                    "7. Перейти в Алерт по транзакции от Клиента №3 и выполнить Action из предусловия" +
                    "8. Проверить отправку сообщения в КАФ по картам клиентов №2 и №3",
            dependsOnMethods = "clientCAF2"
    )
    public void clientCAF3() {
        time.add(Calendar.SECOND, 20);
        Transaction transService = getServicePayment();
        TransactionDataType transactionDataService = transService.getData().getTransactionData();
        transactionDataService
                .getClientIds().withDboId(clientIds.get(2));
        transactionDataService
                .getServicePayment()
                .withSourceCardNumber(PAN1);
        sendAndAssert(transService);

        getIC().locateAlerts()
                .openFirst()
                .action("Разблокировать все карты 167-ФЗ");

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(3)[3], CARD_ID[7]);
        assertEquals(res.getFildsValuesLog(3)[1], "KAF");
        assertEquals(res.getFildsValuesLog(8)[3], CARD_ID[5]);
        assertEquals(res.getFildsValuesLog(8)[1], "KAF");
        assertEquals(res.getFildsValuesLog(19)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(19)[1], "KAF");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withProviderName(providerName)
                .withServiceName(serviceName);
        return transaction;
    }
}