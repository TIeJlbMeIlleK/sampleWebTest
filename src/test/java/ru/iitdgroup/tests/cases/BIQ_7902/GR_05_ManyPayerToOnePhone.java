package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.intellinx.dbo.transaction.AdditionalFieldType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

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


public class GR_05_ManyPayerToOnePhone extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Тимур", "Киров", "Семенович"}, {"Зина", "Птушкина", "Ильинична"},
            {"Федор", "Бондарчук", "Григорьевич"}, {"Илья", "Кисов", "Васильевич"}, {"Тимур", "Киров", "Батырович"},
            {"Семен", "Тиков", "Гаврилович"}, {"Ирина", "Парькина", "Семеновна"}};

    private static final String RULE_NAME = "R01_GR_05_ManyPayerToOnePhone";
    private static String transactionID1;
    private static String transactionID2;

    private final String recipient1 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient2 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient3 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient4 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient5 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient6 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String recipient7 = "9" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);

    @Test(
            description = "1. Включить правило"
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
                .fillCheckBox("Проверка регулярных:", true)
                .save()
                .sleep(20);
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        System.out.println(recipient1);
        try {
            for (int i = 0; i < 7; i++) {
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
            description = "Провести транзакцию № 1 для Клиента № 1, сумма 2, " +
                    "регулярная, Получатель № 1 (Version = 9997, transactionID = 10)",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9997L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 2 для Клиента № 1, сумма 998, " +
                    "Получатель № 1 (Version = 9997, transactionID = 11)",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9997L)
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(998))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 3 для Клиента № 2, сумма 1001, " +
                    "регулярная, Получатель № 2 (Version = 9998, transactionID = 12)",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9998L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = " Провести транзакцию № 4, для Клиента № 3, сумма 10, " +
                    "регулярные, Получатель № 3 (Version = 9901, transactionID = 1)",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9901L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient3);
        transactionID1 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = " Провести транзакцию № 5 для Клиента № 3, сумма 10, " +
                    "регулярные, Получатель № 3 (Version = 9901, transactionID = 2)",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9901L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient3);
        transactionID2 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = " Провести транзакцию № 6 для Клиента № 3, сумма 10, " +
                    "регулярные, Получатель № 3 (Version = 9902, transactionID = 2)",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID2)
                .withVersion(9902L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient3);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Количество транзакций больше параметра Длина серии");
    }

    @Test(
            description = "Провести транзакцию № 7 для Клиента № 4 сумма 10, " +
                    "регулярная, Получатель № 4 (Version = 9902, transactionID = 3)",
            dependsOnMethods = "step6"
    )

    public void step7() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9902L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient4);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 8 для Клиента № 4, сумма 10, спустя 11 минут после транзакции № 7, " +
                    "регулярная, Получатель № 4 (Version = 9902, transactionID = 4)",
            dependsOnMethods = "step7"
    )

    public void step8() {
        time.add(Calendar.MINUTE, 12); //прибавляет к текущей дате и времени минуты
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9902L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient4);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 9 для Клиента № 5, сумма 2, " +
                    "Получатель № 5 (Version = 9903, transactionID = 1)",
            dependsOnMethods = "step8"
    )

    public void step9() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID1)
                .withVersion(9903L)
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient5);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 10 для Клиента № 5, сумма 998, " +
                    "регулярная, Получатель № 5 (Version = 9903, transactionID = 2)",
            dependsOnMethods = "step9"
    )

    public void step10() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID2)
                .withVersion(9903L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(998))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient5);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 11 для Клиента № 6, сумма 500, " +
                    "регулярная, Получатель № 6 (Version = 9905, transactionID = 1)",
            dependsOnMethods = "step10"
    )

    public void step11() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID1)
                .withVersion(9905L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient6);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 12 для Клиента № 7, сумма 500, " +
                    "регулярная, Получатель № 7 (Version = 9905, transactionID = 2)",
            dependsOnMethods = "step11"
    )

    public void step12() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID2)
                .withVersion(9905L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(6));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient7);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}