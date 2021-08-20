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
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_101_CC_Fake_Passport extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_101_CC_FakePassport";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
//    private final GregorianCalendar time = new GregorianCalendar(2021, Calendar.JANUARY, 11, 15, 1, 0);
    private final List<String> clientIds = new ArrayList<>();
    private String dboId_1 = ThreadLocalRandom.current().nextInt(999999999) + "";
    private String dboId_2 = ThreadLocalRandom.current().nextInt(999999999) + "";

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private GregorianCalendar timeOf1stTransaction;
    private GregorianCalendar timeOf2ndTransaction;

//    TODO требуется проверить после исправления BIQ6976-37


//    time.add(Calendar.DAY_OF_MONTH, -2); // текущая дата минус 2 дня
    // time.add(Calendar.MINUTE, 1); //прибавляет к текущей дате и времени одну минуту

//    time2 = (GregorianCalendar) time.clone(); //запоминает или клонирует дату в нужной транзакции

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
                .fillInputText("Время с момента выпуска карты:","5")
                .fillInputText("Время с момента подключения ДБО:","5")
                .fillInputText("Имя продукта карты:","5555")
                .save()
                .detachAll("Сумма по каждому типу транзакций")
                .attachTransaction("Сумма по каждому типу транзакций","Запрос на выдачу кредита", "3000")
                .sleep(30);
        getIC().close();
    }

    @Test(
            description = "Передать карточку клиента КАФ",
            dependsOnMethods = "enableRules"
    )
    public void clientCaf() {
        commandServiceMock.run();
        try {
            String clientResponse = getRabbit().getCafClientResponse();
            JSONObject json = new JSONObject(clientResponse);
            json.put("clientId", dboId_1);
            json.put("cardId", dboId_1);
            json.put("pan", dboId_1);
            json.put("account", dboId_1);
            json.put("productName", "5555");
            json.put("cardIssueDate", System.currentTimeMillis() / 1000L);
            String newStr = json.toString();
            getRabbit().setCafClientResponse(newStr);
            getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafClientQueueName())
                    .sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "clientCaf"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId_1)
                        .getClientIds()
                        .withLoginHash(dboId_1)
                        .withDboId(dboId_1)
                        .withCifId(dboId_1)
                        .withExpertSystemId(dboId_1)
                        .withEksId(dboId_1)
                        .getAlfaIds()
                        .withAlfaId(dboId_1);
                sendAndAssert(client);
                clientIds.add(dboId_1);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Передать из КАФ нефинансовое событие Клиента № 1 (координаты терминала Москвы 55.756655,37.595151 , Координаты VEReq отсутствуют)).",
            dependsOnMethods = "clientCaf"
    )

    public void sendTransaction() {
        GregorianCalendar timeForTran1 = new GregorianCalendar();
//        timeOf1stTransaction = (GregorianCalendar) time.clone();
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(timeForTran1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(timeForTran1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientIds()
                .getAlfaIds()
                .withAlfaId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .setAmountInSourceCurrency(new BigDecimal(3001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RULE_TRIGGERED);
    }

    @Test(
            description = "Получить из КАФ карточку Клиента № 2 с информацией о выпуске ему новой карты 1 типа InstantIssue.",
            dependsOnMethods = "sendTransaction"
    )
    public void clientCaf2() {
        try {
            for (int i = 0; i < 1; i++) {
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId_2)
                        .getClientIds()
                        .withLoginHash(dboId_2)
                        .withDboId(dboId_2)
                        .withCifId(dboId_2)
                        .withExpertSystemId(dboId_2)
                        .withEksId(dboId_2)
                        .getAlfaIds()
                        .withAlfaId(dboId_2);
                sendAndAssert(client);
                clientIds.add(dboId_2);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            String clientResponse = getRabbit().getCafClientResponse();
            JSONObject json = new JSONObject(clientResponse);
            json.put("clientId", dboId_2);
            json.put("cardId", dboId_2);
            json.put("pan", dboId_2);
            json.put("account", dboId_2);
            json.put("productName", "5555");
            json.put("cardIssueDate", System.currentTimeMillis() / 1000L);
            String newStr = json.toString();
            getRabbit().setCafClientResponse(newStr);
            getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafClientQueueName())
                    .sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
        getRabbit().close();
    }

    @Test(
            description = "Провести транзакцию  № 3 от клиента № 1 \"Запрос на выдачу кредита\", такую что расстояние между координатами События КАФ и транзакции более 501 км и скорость больше аномальной (800 км/ч) (ip-адрес Новосибирска (5.128.16.120)) через 1 секунду.",
            dependsOnMethods = "clientCaf2"
    )
    public void clientDbo2() {
        try {
            for (int i = 0; i < 1; i++) {
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId_2)
                        .getClientIds()
                        .withLoginHash(dboId_2)
                        .withDboId(dboId_2)
                        .withCifId(dboId_2)
                        .withExpertSystemId(dboId_2)
                        .withEksId(dboId_2)
                        .getAlfaIds()
                        .withAlfaId(dboId_2);
                client
                        .getData()
                        .getClientData().getContactInfo()
                        .getContact()
                        .get(1)
                        .setValue("+79050100355");
                sendAndAssert(client);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Менее чем через 5 минут от момента изменения номера провести транзакции от клиента № 2:\n" +
                    "-  № 2  \"Запрос на выдачу кредита\" на сумму 3001 рубль.",
            dependsOnMethods = "clientCaf2"
    )
    public void sendTransaction2() {
        GregorianCalendar timeForTran2 = new GregorianCalendar();
//        timeOf1stTransaction = (GregorianCalendar) time.clone();
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(timeForTran2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(timeForTran2))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientIds()
                .getAlfaIds()
                .withAlfaId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .setAmountInSourceCurrency(new BigDecimal(3001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RULE_TRIGGERED);
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
