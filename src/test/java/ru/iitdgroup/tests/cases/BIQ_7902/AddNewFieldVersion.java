package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import static org.testng.Assert.assertEquals;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AddNewFieldVersion extends RSHBCaseTest {

    private static final String RULE_NAME = "";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String[][] names = {{"Игорь", "Любушкин", "Иванович"}};
    private static String transactionID;

    @Test(
            description = "Создание клиентов"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time))
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
            description = "Отправить транзакции из предусловия на обработку в САФ" +
                    "далее Отправить транзакции из предусловие, с новым значением в поле Version, но старым transactionID",
            dependsOnMethods = "addClients"
    )

    public void step1() throws InterruptedException {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L);
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Thread.sleep(2000);
        String[] res = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res[0], "1");

        Transaction transaction2 = getTransaction();
        transaction2.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(2L);
        sendAndAssert(transaction2);

        Thread.sleep(2000);
        String[] res1 = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res1[0], "2");
    }

    @Test(
            description = "Отправить транзакции из предусловия на обработку в САФ с другим типом транзакции" +
                    "далее Отправить транзакции из предусловие, с новым значением в поле Version, но старым transactionID",
            dependsOnMethods = "step1"
    )

    public void step2() throws InterruptedException {
        Transaction transaction = getTransactionPhone();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L);
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Thread.sleep(2000);
        String[] res = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res[0], "1");

        Transaction transaction2 = getTransactionPhone();
        transaction2.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(2L);
        sendAndAssert(transaction2);

        Thread.sleep(2000);
        String[] res1 = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res1[0], "2");
    }

    @Test(
            description = "Отправить транзакции из предусловия на обработку в САФ с другим типом транзакции" +
                    "далее Отправить транзакции из предусловие, с новым значением в поле Version, но старым transactionID",
            dependsOnMethods = "step2"
    )

    public void step3() throws InterruptedException {
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L);
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Thread.sleep(2000);
        String[] res = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res[0], "1");

        Transaction transaction2 = getTransactionOuterTransfer();
        transaction2.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(2L);
        sendAndAssert(transaction2);

        Thread.sleep(2000);
        String[] res1 = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res1[0], "2");
    }

    @Test(
            description = "Отправить транзакции из предусловия на обработку в САФ с другим типом транзакции" +
                    "далее Отправить транзакции из предусловие, с новым значением в поле Version, но старым transactionID",
            dependsOnMethods = "step3"
    )

    public void step4() throws InterruptedException {
        Transaction transaction = getTransactionQRCode();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L);
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Thread.sleep(2000);
        String[] res = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res[0], "1");

        Transaction transaction2 = getTransactionQRCode();
        transaction2.getData().getTransactionData()
                .withTransactionId(transactionID)
                .withVersion(2L);
        sendAndAssert(transaction2);

        Thread.sleep(2000);
        String[] res1 = getFieldWithLastId("PAYMENT_TRANSACTION", "DOCUMENT_VERSION");
        assertEquals(res1[0], "2");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhone() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        transactionDataType
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        transactionDataType
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        transactionDataType
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }

    private Transaction getTransactionQRCode() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        transactionDataType
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }
}
