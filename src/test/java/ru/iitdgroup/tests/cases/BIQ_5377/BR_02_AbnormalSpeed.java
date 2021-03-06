package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

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
                .save().sleep(25);
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
            description = "Провести транзакцию № 1 \"Платеж по QR-коду через СБП\" с текущей датой со временем в \"DocumentSaveTimeStamp\" 00:00:00.000",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
        time.add(Calendar.MINUTE, -15);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Промежуток времени между транзакциями больше интервала");
    }

    @Test(
            description = "Провести регулярную транзакцию № 2 \"Платеж по QR-коду через СБП\" со временем 00:00:55.000",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        time.add(Calendar.SECOND, 55);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применяется для регулярных транзакций");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Платеж по QR-коду через СБП\" со временем 00:01:05.000",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        time.add(Calendar.SECOND, 10);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Промежуток времени между транзакциями больше интервала");
    }

    @Test(
            description = "Провести транзакцию № 4 \"Платеж по QR-коду через СБП\"  со временем 00:02:05.000",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        time.add(Calendar.SECOND, 60);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Промежуток времени между транзакциями меньше интервала");
    }

    @Test(
            description = "Провести транзакцию № 5 \"Платеж по QR-коду через СБП\" со временем 00:03:06.000",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {
        time.add(Calendar.SECOND, 61);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Промежуток времени между транзакциями больше интервала");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(9500));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}
