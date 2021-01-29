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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class GR_102_CC_Avito1 extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_GR_102_CC_Avito1";
    private static String TABLE_NAME = "(Policy_parameters) Блоки сценариев";

    private final GregorianCalendar time = new GregorianCalendar(2021, Calendar.JANUARY, 20, 11, 52, 0);

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Эльмира", "Пирожкова", "Викторовна"}};
    private static String[] login = {new RandomString(5).nextString(), new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};

    private static final String PAN_ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String CARD_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);
    private static final String DATA_TIME = "1611132600";//не менять! сравнивает с time1

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период времени сценария:", "5")
                .fillInputText("Сумма транзакции:", "3000")
                .save()
                .sleep(10);
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                Client client = new Client("testCases/Templates/client.xml");

                if (i == 0) {
                    client.getData().getClientData().getClient().getClientIds().getAlfaIds()
                            .withAlfaId(dboId);
                } else {
                    client.getData().getClientData().getClient().getClientIds().withAlfaIds(null);
                }
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
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId);

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
                    "2. Передать из КАФ нефинансовое событие с кодом = запрос баланса №1",
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
            json.put("cardId", CARD_ID);
            json.put("pan", PAN_ACCOUNT);
            json.put("account", PAN_ACCOUNT);
            String newStr = json.toString();
            getRabbit().setCafClientResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);

            String cafNonFinanceResponse = getRabbit()
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafFactsQueueName())
                    .getCafNotFinanceResponse();
            JSONObject js = new JSONObject(cafNonFinanceResponse);
            js.put("dateTime", DATA_TIME);
            js.put("cardholderId", clientIds.get(0));
            js.put("cardId", CARD_ID);
            js.put("pan", PAN_ACCOUNT);
            js.put("account", PAN_ACCOUNT);
            String newStr1 = js.toString();
            getRabbit().setCafNotFinanceResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_NOT_FINANCE_RESPONSE);
            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Менее чем через 5 минут провести транзакцию №1 от клиента №1 \"Запрос на выдачу кредита\" на сумму 3001 руб",
            dependsOnMethods = "addClients"
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(3001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Подозрение на развод на Авито");
    }

    @Test(
            description = "Более чем через 5 минут провести транзакцию №2 от клиента №1 \"Запрос на выдачу кредита\" на сумму 3001 руб",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(3001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Транзакция не подозрительная");
    }

    @Test(
            description = "Менее чем через 5 минут провести транзакцию №3 от клиента №1 \"запрос на выдачу кредита\" на сумму 1000 руб",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        time.add(Calendar.MINUTE, 2);
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сумма транзакции ниже или равна установленной правилом: 1000");
    }

    @Test(
            description = "Более чем через 5 минут провести транзакцию №4 от клиента №1 \"Запрос на выдачу кредита\" на сумму 1000 руб",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        time.add(Calendar.MINUTE, 6);
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сумма транзакции ниже или равна установленной правилом: 1000");
    }

    @Test(
            description = "Передать из ДБО данные Клиента №2 , не содержащие ссылки на AlfaID" +
                    "Провести транзакцию №5 от клиента №3 \"Запрос на выдачу кредита\" насумму 3001 руб",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {

        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(3001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "У клиента отсутствуют alfaId");
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