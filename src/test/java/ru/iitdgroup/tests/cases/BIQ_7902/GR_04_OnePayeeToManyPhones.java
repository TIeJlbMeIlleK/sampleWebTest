package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
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

//TODO до запуска теста должен быть заполнен справочник (Rule_tables) Системы электронных кошельков

public class GR_04_OnePayeeToManyPhones extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Тимур", "Киров", "Семенович"}, {"Зина", "Птушкина", "Ильинична"},
            {"Федор", "Бондарчук", "Григорьевич"}, {"Илья", "Кисов", "Васильевич"}, {"Тимур", "Киров", "Батырович"}, {"Семен", "Тиков", "Гаврилович"}};

    private static final String RULE_NAME = "R01_GR_04_OnePayerToManyPhones";
    private static String transactionID10;
    private static String transactionID11;

    private final static String recipient1 = "9100888410";
    private final static String recipient2 = "9100888411";
    private final static String recipient4 = "9100888413";
    private final static String recipient5 = "9100888414";
    private final static String recipient6 = "9100888415";
    private final static String recipient8 = "9100888417";
    private final static String recipient9 = "9100888418";
    private final static String recipient10 = "9100888419";
    private final static String recipient12 = "9100888421";
    private final static String recipient13 = "9100888422";
    private final static String recipient15 = "9100888424";
    private final static String recipient16 = "9100888425";

    @Test(
            description = "1. Включить правило R01_GR_04_OnePayerToManyPhones"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
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
        try {
            for (int i = 0; i < 6; i++) {
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
            description = "Провести транзакцию № 1 для Клиента № 1, сумма 2, регулярная (Version = 9999, transactionID = 10)",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9999L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").withValue(recipient1);

        transactionID10 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 2 для Клиента № 1, сумма 998 (Version = 9999, transactionID = 11)",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9999L)
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(998))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient2);
        transactionID11 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 3 для Клиента № 2, сумма 1000, регулярная (Version = 9990, transactionID = 10)",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9990L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient4);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 4, для Клиента № 3, сумма 10, " +
                    "регулярные (Version = 9991, transactionID = 10)",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9991L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient5);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 5, для Клиента № 3, сумма 10, " +
                    "регулярные (Version = 9991, transactionID = 11)",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID11)
                .withVersion(9991L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient6);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 6, для Клиента № 3, сумма 10, " +
                    "регулярные (Version = 9992, transactionID = 10)",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9992L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient8);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Количество транзакций больше параметра Длина серии");
    }

    @Test(
            description = "Провести транзакцию № 7 для Клиента № 4 сумма 10, " +
                    "регулярная (Version = 9993, transactionID = 10)",
            dependsOnMethods = "step6"
    )

    public void step7() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9993L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient9);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 8 для Клиента № 4 сумма 10, спустя 11 минут после транзакции № 7" +
                    "регулярная (Version = 9993, transactionID = 11)",
            dependsOnMethods = "step7"
    )

    public void step8() {
        time.add(Calendar.MINUTE, 12); //прибавляет к текущей дате и времени минуты
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID11)
                .withVersion(9993L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient10);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 9 для Клиента № 5, сумма 2 (Version = 9994, transactionID = 10)",
            dependsOnMethods = "step8"
    )

    public void step9() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9994L)
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient12);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 10 для Клиента № 5, сумма 998, регулярная (Version = 9995, transactionID = 10)",
            dependsOnMethods = "step9"
    )

    public void step10() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID10)
                .withVersion(9995L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(998))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient13);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 11 для Клиента № 6, сумма 500, регулярная  (Version = 9995, transactionID = 11)",
            dependsOnMethods = "step10"
    )

    public void step11() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID11)
                .withVersion(9995L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient15);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В выборке только анализируемая транзакция");
    }

    @Test(
            description = "Провести транзакцию № 12 для Клиента № 6, сумма 500, регулярная (Version = 9996, transactionID = 11)",
            dependsOnMethods = "step11"
    )

    public void step12() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID11)
                .withVersion(9996L)
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .getAdditionalField().get(0).withId("ACCOUNT").withName("Номер телефона").setValue(recipient16);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        String[] provider = getFieldWithLastId("LIST_EWALLETS", "PAYEE");
        String[] service = getFieldWithLastId("LIST_EWALLETS", "NAME_OF_THE_SERVICE");

        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData().getServicePayment()
                .withProviderName(provider[0])
                .withServiceName(service[0]);
        return transaction;
    }
}