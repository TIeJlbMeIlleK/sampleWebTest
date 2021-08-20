package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_03_SeriesOneToMany extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Тимур", "Киров", "Семенович"}, {"Зина", "Птушкина", "Ильинична"},
            {"Федор", "Бондарчук", "Григорьевич"}, {"Илья", "Кисов", "Васильевич"}};

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private static String transactionID;

    @Test(
            description = "1. Включить правило R01_GR_03_SeriesOneToMany"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Сумма серии:", "1000")
                .save()
                .sleep(20);
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 4; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
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
            description = "Провести транзакцию № 1 \"Перевод на карту\" для Клиента № 1, регулярная, сумма 1001 (Version = 8888, transactionID = 8)",
            dependsOnMethods = "addClient"
    )

    public void step1() {

        Transaction transaction = getTransactionCard();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(8888L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод по номеру телефона\" для Клиента № 1, сумма 999 (Version = 8888, transactionID = 9)",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransactionPhone();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(8888L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(999));

        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на счет\" для Клиента № 1, сумма 2 (Version = 8889, transactionID = 9)",
            dependsOnMethods = "step2"
    )

    public void step3() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(8889L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Общая сумма транзакций больше допустимой величины");
    }


    @Test(
            description = " Провести транзакцию № 4 \"Перевод на счет\" для Клиента № 2, сумма 1001 (Version = 8899, transactionID = 10)",
            dependsOnMethods = "step3"
    )

    public void step4() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(8899L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод по номеру телефона\" для Клиента № 3," +
                    "сумма 10 (Version = 8890, transactionID = 11)," +
                    "(Version = 8891, transactionID = 11), (Version = 8892, transactionID = 11)",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransactionPhone();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(8890L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод по номеру телефона\" для Клиента № 3," +
                    "сумма 10 (Version = 8890, transactionID = 11)," +
                    "(Version = 8891, transactionID = 11), (Version = 8892, transactionID = 11)",
            dependsOnMethods = "step5"
    )

    public void step6() {

        Transaction transaction = getTransactionPhone();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(8891L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод по номеру телефона\" для Клиента № 3," +
                    "сумма 10 (Version = 8890, transactionID = 11)," +
                    "(Version = 8891, transactionID = 11), (Version = 8892, transactionID = 11)",
            dependsOnMethods = "step6"
    )
    public void step7() {

        Transaction transaction = getTransactionPhone();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(8892L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Количество транзакций больше допустимой длины серии");
    }

    @Test(
            description = "Провести транзакцию № 8, 9 \"Перевод на карту\" для Клиента № 4 сумма 10 " +
                    "(Version = 8893, transactionID = 12), (Version = 8894, transactionID = 12)",
            dependsOnMethods = "step7"
    )

    public void step8() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionCard();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(8893L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 8, 9 \"Перевод на карту\" для Клиента № 4 сумма 10 " +
                    "(Version = 8893, transactionID = 12), (Version = 8894, transactionID = 12)",
            dependsOnMethods = "step8"
    )

    public void step9() {
        Transaction transaction = getTransactionCard();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(8894L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = " Провести транзакцию № 10 \"Перевод на карту\" для Клиента № 4, сумма 10, " +
                    "спустя 11 минут после транзакции № 8 (Version = 8895, transactionID = 12)",
            dependsOnMethods = "step9"
    )

    public void step10() {
        time.add(Calendar.MINUTE, 12); //прибавляет к текущей дате и времени одну минуту
        Transaction transaction = getTransactionCard();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(8895L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhone() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionCard() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}