package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_07_BigTransfer extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private HashMap<String, Object> map = new HashMap<>();
    private HashMap<String, Object> map1 = new HashMap<>();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Кира", "Кукушкин", "Семенович"}};

    private static final String RULE_NAME = "R01_GR_07_BigTransfer";
    private static final String TYPE_TSP2 = new RandomString(8).nextString();


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "1. Включить правило R01_GR_07_BigTransfer",
            dependsOnMethods = "addClient"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Контроль лимита  (пример 0.05):", "0,1")
                .save()
                .sleep(15);
        getIC().close();
    }

    @Test(
            description = "Провести транзакции № 1 \"Платеж по QR-коду через СБП\", сумма 9.01, лимит 10",
            dependsOnMethods = "enableRules"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withLimit(BigDecimal.valueOf(10));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(9.01))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Осуществление крупного перевода средств");
    }

    @Test(
            description = "Провести транзакции № 2 \"Платеж по QR-коду через СБП\", сумма 8, лимит 10",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withLimit(BigDecimal.valueOf(10));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(8))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакции № 3 \"Платеж по QR-коду через СБП\", сумма 9.01, лимит не указан",
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(9.01))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }


    @Test(
            description = "Провести транзакции № 4 \"Платеж по QR-коду через СБП\", сумма 9.01, лимит 10, " +
                    "указан признак \"Подтверждение операций сверх лимита\" " +
                    "(ConfirmationBigTransfer =CC (анг))",
            dependsOnMethods = "step3"
    )

    public void step4() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withLimit(BigDecimal.valueOf(10));
        transactionData
                .withConfirmationBigTransfer("CC")
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(9.01))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
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