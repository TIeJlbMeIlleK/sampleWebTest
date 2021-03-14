package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
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


public class BR_02_AbnormalSpeed extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_BR_02_AbnormalSpeed";
    private static String transactionID1;

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Интервал между операциями (в секундах, пример 2):", "60")
                .save().sleep(10);
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "Провести транзакцию № 1 \"Оплата услуг\" с текущей датой со временем " +
                    "в \"DocumentSaveTimeStamp\" 00:00:00.000 (Version = 5555, transactionID = 5)",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(5555L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionID1 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Промежуток времени между транзакциями больше интервала");
    }

    @Test(
            description = "Провести регулярную транзакцию № 2 \"Оплата услуг\" " +
                    "со временем 00:00:55.000 (Version = 5555 transactionID = 4)",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        time.add(Calendar.SECOND, 55);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(5555L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применяется для регулярных транзакций");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Оплата услуг\" " +
                    "со временем 00:01:05.000 (Version = 5554, transactionID = 5)",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        time.add(Calendar.SECOND, 10);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID1)
                .withVersion(5554L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Промежуток времени между транзакциями больше интервала");
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод между счетами\"" +
                    "со временем 00:02:04.000 (Version = 5553, transactionID = 5)",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        time.add(Calendar.SECOND, 59);
        Transaction transaction = getTransactionAccount();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID1)
                .withVersion(5553L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Промежуток времени между транзакциями меньше интервала");
    }

    @Test(
            description = "Провести транзакцию № 5 \"Оплата услуг\" " +
                    "со временем 00:02:05.000 (Version = 5553, transactionID = 6)",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(5553L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Промежуток времени между транзакциями меньше интервала");
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

    private Transaction getTransactionAccount() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
