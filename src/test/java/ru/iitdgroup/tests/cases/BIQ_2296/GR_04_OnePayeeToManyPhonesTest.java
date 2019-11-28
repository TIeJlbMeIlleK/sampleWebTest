package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.AdditionalFieldType;
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

public class GR_04_OnePayeeToManyPhonesTest extends RSHBCaseTest {

    private static final String PHONE1 = "9122222221";
    private static final String PHONE2 = "9122222222";
    private static final String PHONE3 = "9122222223";
    private static final String PHONE4 = "9122222224";
    private static final String PHONE_SERVICE_NAME = "PHONE";
    private static final String QIWI_SERVICE_NAME = "QIWI";
    private static final String TV_SERVICE_NAME = "TV";
    private static final String RULE_NAME = "R01_GR_04_OnePayerToManyPhones";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    private GregorianCalendar transaction8GC;

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Сумма серии:", "1000")
                .fillCheckBox("Проверка регулярных:", false)
                .save();

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate().sleep(15);

        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 4; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1 по оплате телефона для Клиента № 1, регулярная, сумма 1001",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withProviderName(PHONE1)
                .withServiceName(PHONE_SERVICE_NAME)
                .withAdditionalField(getPhoneField(PHONE1))
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 2  по оплате телефона для Клиента № 1, сумма 999",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withProviderName(PHONE1)
                .withServiceName(PHONE_SERVICE_NAME)
                .withAdditionalField(getPhoneField(PHONE1))
                .withAmountInSourceCurrency(BigDecimal.valueOf(999));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_EMPTY);
    }

    @Test(
            description = "Провести транзакцию № 3 по оплате кошелька QIWI для Клиента № 1, сумма 1",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withProviderName(QIWI_SERVICE_NAME)
                .withServiceName(QIWI_SERVICE_NAME)
                .withAdditionalField(getPhoneField(PHONE1))
                .withAmountInSourceCurrency(BigDecimal.valueOf(1));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 4 по оплате ТВ для Клиента № 1, сумма 1",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withProviderName(TV_SERVICE_NAME)
                .withServiceName(TV_SERVICE_NAME)
                .withAdditionalField(getPhoneField(PHONE1))
                .withAmountInSourceCurrency(BigDecimal.valueOf(1));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 5 для Клиента № 2, сумма 1000",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(1));
        transactionData.getServicePayment()
                .withProviderName(PHONE2)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 6, 7, 8 для Клиента № 3, сумма 10",
            dependsOnMethods = "step5"
    )
    public void step6() {
        for (int i = 6; i <= 8; i++) {
            time.add(Calendar.MINUTE, 1);
            Transaction transaction = getTransaction();
            TransactionDataType transactionData = transaction.getData().getTransactionData();
            transactionData.getClientIds().withDboId(clientIds.get(2));
            transactionData
                    .getServicePayment()
                    .withServiceName(PHONE3)
                    .withProviderName(PHONE3)
                    .withAdditionalField(getPhoneField(PHONE3))
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));

            sendAndAssert(transaction);
            switch (i) {
                case 6:
                    assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_EMPTY);
                    break;
                case 7:
                    // Нужно запомнить время 8й транзакции, чтобы отправить 11ую транзакцию с правильным временем
                    transaction8GC = (GregorianCalendar) time.clone();
                    assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
                    break;
                case 8:
                    assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGTH);
                    break;
            }
        }
    }

    @Test(
            description = "Провести транзакцию № 9, 10 для Клиента № 4 сумма 10",
            dependsOnMethods = "step6"
    )
    public void step7() {
        for (int i = 9; i <= 10; i++) {
            time.add(Calendar.MINUTE, 5);
            Transaction transaction = getTransaction();
            TransactionDataType transactionData = transaction.getData().getTransactionData();
            transactionData.getClientIds().withDboId(clientIds.get(3));
            transactionData.getServicePayment()
                    .withProviderName(PHONE4)
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));

            sendAndAssert(transaction);
            assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        }
    }

    @Test(
            description = "Провести транзакцию № 11 для Клиента № 4, сумма 10, спустя 11 минут после транзакции № 8",
            dependsOnMethods = "step7"
    )
    public void step8() {
        transaction8GC.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getServicePayment()
                .withProviderName(PHONE4)
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private AdditionalFieldType getPhoneField(String phone) {
        return new AdditionalFieldType()
                .withId("account")
                .withName("account")
                .withValue(phone);
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
