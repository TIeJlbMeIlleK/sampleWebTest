package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.common.ClientIdsType;
import ru.iitdgroup.intellinx.dbo.transaction.AdditionalFieldType;
import ru.iitdgroup.intellinx.dbo.transaction.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.dbdriver.Database;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GR04OnePayeeToManyPhonesTest extends RSHBCaseTest {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String PHONE1 = "+791222222221";
    private static final String PHONE2 = "+791222222222";
    private static final String PHONE3 = "+791222222223";
    private static final String PHONE4 = "+791222222224";

    private static final AdditionalFieldType PHONE_FIELD = OBJECT_FACTORY
            .createAdditionalFieldType()
            .withId("account")
            .withName("account")
            .withValue(PHONE1);

    private final GregorianCalendar saveGC = new GregorianCalendar(2019, Calendar.JUNE, 1, 1, 0, 0);
    private GregorianCalendar transaction8GC;

    private final List<Long> transactionIds = new ArrayList<>();

    private Transaction getTransaction() {
        try {
            Transaction transaction = new Transaction("testCases/GR04OnePayeeToManyPhones/tran.xml");
            transaction.getData()
                    .getTransactionData()
                    .withSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "")
                    .withDocumentNumber(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
            return transaction;
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule("R01_GR_04_OnePayerToManyPhones")
                .activate()
                .sleep(3);

        getIC().close();
    }

    @Test(
            description = "Создаем клиентов 1, 2, 3, 4",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            Client client = new Client("testCases/GR04OnePayeeToManyPhones/client.xml");
            DBOAntiFraudWS send = send(client);
            assertTrue(send.isSuccessResponse());
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

        transaction.getData().getTransactionData()
                .withRegular(true)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .getServicePayment()
                .withProviderName(PHONE1)
                .withServiceName("PHONE")
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001))
                .withAdditionalField(PHONE_FIELD);

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        saveGC.add(Calendar.MINUTE, 10);
    }

    @Test(
            description = "Провести транзакцию № 2  по оплате телефона для Клиента № 1, сумма 999",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();

        transaction.getData().getTransactionData()
                .withRegular(false)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .getServicePayment()
                .withProviderName(PHONE2)
                .withServiceName("PHONE")
                .withAdditionalField(PHONE_FIELD)
                .withAmountInSourceCurrency(BigDecimal.valueOf(999));

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        saveGC.add(Calendar.MINUTE, 10);
    }

    @Test(
            description = "Провести транзакцию № 3 по оплате кошелька QIWI для Клиента № 1, сумма 1",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();

        transaction.getData().getTransactionData()
                .withRegular(false)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .getServicePayment()
                .withProviderName(PHONE1)
                .withServiceName("QIWI")
                .withAmountInSourceCurrency(BigDecimal.valueOf(1));

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        saveGC.add(Calendar.MINUTE, 10);
    }

    @Test(
            description = "Провести транзакцию № 4 по оплате ТВ для Клиента № 1, сумма 1",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();

        transaction.getData().getTransactionData()
                .withRegular(false)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .getServicePayment()
                .withProviderName(PHONE1)
                .withServiceName("TV")
                .withAmountInSourceCurrency(BigDecimal.valueOf(1));

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        saveGC.add(Calendar.MINUTE, 10);
    }

    @Test(
            description = "Провести транзакцию № 5 для Клиента № 2, сумма 1000",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();

        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC));

        transactionData.getServicePayment()
                .withProviderName(PHONE2)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        saveGC.add(Calendar.MINUTE, 10);
    }

    @Test(
            description = "Провести транзакцию № 6, 7, 8 для Клиента № 3, сумма 10",
            dependsOnMethods = "step5"
    )
    public void step6() {
        for (int i = 6; i <= 8; i++) {

            Transaction transaction = getTransaction();

            TransactionDataType transactionData = transaction.getData().getTransactionData()
                    .withRegular(false)
                    .withType(TransactionType.SERVICE_PAYMENT)
                    .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                    .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC));

            transactionData.getServicePayment()
                    .withProviderName(PHONE3)
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));

            DBOAntiFraudWS response = send(transaction);
            assertTrue(response.isSuccessResponse());

            transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

            saveGC.add(Calendar.MINUTE, 10);

            if (i == 8) {
                transaction8GC = (GregorianCalendar) saveGC.clone();
            }
        }
    }

    @Test(
            description = "Провести транзакцию № 9, 10 для Клиента № 4 сумма 10",
            dependsOnMethods = "step6"
    )
    public void step7() {
        for (int i = 9; i <= 10; i++) {
            Transaction transaction = getTransaction();

            TransactionDataType transactionData = transaction.getData().getTransactionData()
                    .withRegular(false)
                    .withType(TransactionType.SERVICE_PAYMENT)
                    .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(saveGC))
                    .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(saveGC));

            transactionData.getServicePayment()
                    .withProviderName(PHONE4)
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));

            DBOAntiFraudWS response = send(transaction);
            assertTrue(response.isSuccessResponse());

            transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

            saveGC.add(Calendar.MINUTE, 10);
        }
    }

    @Test(
            description = "Провести транзакцию № 11 для Клиента № 4, сумма 10, спустя 11 минут после транзакции № 8",
            dependsOnMethods = "step7"
    )
    public void step8() {
        Transaction transaction = getTransaction();

        transaction8GC.add(Calendar.MINUTE, 10);

        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withType(TransactionType.SERVICE_PAYMENT)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(transaction8GC))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(transaction8GC));
        transactionData.getServicePayment()
                .withAdditionalField(PHONE_FIELD.withValue(PHONE4))
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));

        DBOAntiFraudWS response = send(transaction);
        assertTrue(response.isSuccessResponse());

        transactionIds.add(Long.valueOf(transaction.getData().getTransactionData().getTransactionId()));

        for (int i = 0; i < transactionIds.size(); i++) {
            System.out.println(String.format("%d %d", i + 1, transactionIds.get(i)));
        }
    }

    @Test(
            description = "Проверить \"Отчет срабатывания правила\"",
            dependsOnMethods = "step8"
    )
    public void step9() throws SQLException {
        Database database = new Database(getProps());
        final String tableName = "PAYMENT_TRANSACTION pt";
        String[][] result = database
                .select()
                .field("iw.EXECUTION_TYPE")
                .field("iw.DESCRIPTION")
                .innerJoin("TRANSACTION_RULE_AUDIT tra", "pt.id", "tra.PAYMENT_FK")
                .innerJoin("INCIDENT_WRAP iw", "tra.id", "iw.TRANSACTION_RULE_AUDIT_FK")
                .from(tableName)
                .with("pt.TRANSACTION_ID", "IN", transactionIds.toString().replaceAll("\\[", "(").replaceAll("\\]", ")"))
                .with("iw.RULE_TITLE", "=", "'R01_GR_04_OnePayerToManyPhones'")
                .setFormula("AND")
                .get();

        assertEquals(result.length, 11);
        final String NOT_TRIGGERED = "NOT_TRIGGERED";
        final String TRIGGERED = "TRIGGERED";
        final Set<Integer> ruleNotApplySet = new HashSet<>();
        ruleNotApplySet.add(2);
        ruleNotApplySet.add(4);
        ruleNotApplySet.add(5);
        ruleNotApplySet.add(6);
        ruleNotApplySet.add(7);
        ruleNotApplySet.add(9);
        ruleNotApplySet.add(10);
        ruleNotApplySet.add(11);
        for (int i = 0; i < result.length; i++) {
            final int currentTransactionNumber = i + 1;
            System.out.println(transactionIds.get(i));
            if (currentTransactionNumber == 1) {
                assertEquals(NOT_TRIGGERED, result[i][0]);
                assertEquals("Правило не применяется для регулярных транзакций", result[i][1]);
            }
            if (ruleNotApplySet.contains(currentTransactionNumber)) {
                assertEquals(NOT_TRIGGERED, result[i][0]);
                assertEquals("Правило не применилось", result[i][1]);
            }
            if (currentTransactionNumber == 3) {
                assertEquals(TRIGGERED, result[i][0]);
                assertEquals("Общая сумма транзакции больше допустимой величины", result[i][1]);
            }
            if (currentTransactionNumber == 8) {
                assertEquals(TRIGGERED, result[i][0]);
                assertEquals("Количество транзакций больше допустимой длины серии", result[i][1]);
            }
        }
    }

    private void setClientId(ClientIdsType clientId, String id) {
        clientId.withCifId(id)
                .withDboId(id)
                .withEksId(id)
                .withExpertSystemId(id)
                .withLoginHash(id)
                .withPcId(id);
    }

}
