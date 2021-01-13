package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_03_SeriesOneToMany extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time_trans5;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Тимур", "Киров", "Семенович"},{"Зина", "Птушкина", "Ильинична"},{"Федор", "Бондарчук", "Григорьевич"}};

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private static final String TYPE_TSP2 = new RandomString(8).nextString();


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "1. Включить правило R01_GR_03_SeriesOneToMany",
            dependsOnMethods = "addClient"
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
                .sleep(15);
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 1 \"Платеж по QR-коду через СБП\" для Клиента № 1, регулярная, сумма 1001",
            dependsOnMethods = "enableRules"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Платеж по QR-коду через СБП\" для Клиента № 1, сумма 999",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(999))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Платеж по QR-коду через СБП\" для Клиента № 1, сумма 1",
            dependsOnMethods = "step2"
    )

    public void step3() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Общая сумма транзакций больше допустимой величины");
    }


    @Test(
            description = "Провести транзакцию № 4 \"Платеж по QR-коду через СБП\" для Клиента № 2, сумма 1001",
            dependsOnMethods = "step3"
    )

    public void step4() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакции № 5, 6, 7 \"Платеж по QR-коду через СБП\" для Клиента № 3, сумма 10",
            dependsOnMethods = "step4"
    )

    public void step5() {
        time_trans5 = (GregorianCalendar) time.clone();//запоминает или клонирует дату в нужной транзакции
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_trans5))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_trans5))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    public void step6() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    public void step7() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Количество транзакций больше допустимой длины серии");
    }

    @Test(
            description = "Провести транзакцию № 10 \"Платеж по QR-коду через СБП\" для Клиента № 3, сумма 10, спустя 11 минут после транзакции № 5",
            dependsOnMethods = "step5"
    )

    public void step8() {
        time_trans5.add(Calendar.MINUTE, 11); //прибавляет к текущей дате и времени одну минуту
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_trans5))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_trans5))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}