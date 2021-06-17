package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_25_SeriesTransfersAndPayments extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Марина", "Крюкова", "Петровна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    public CommandServiceMock commandServiceMock = new CommandServiceMock(8050);

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("\"Проверка соблюдения требования: в серии есть не менее одной транзакции типа «Перевод на счёт другому лицу» " +
                "или «Перевод на карту другому лицу» и не менее одной транзакции «Оплата услуг»\" -- BIQ2370" + " ТК№22");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:", "60")
                .fillInputText("Сумма оплаты услуг:", "2000")
                .fillInputText("Сумма серии:", "2000")
                .save()
                .sleep(20);
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
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
            description = "Произвести транзакцию 1 Перевод на карту другому лицу от Клиента 1, сумма 1000",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -10);
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 2 Перевод по номеру телефона от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 3 Оплата услуг от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 4 Оплата услуг от Клиента 2, сумма 1000",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time2.add(Calendar.MINUTE, -9);
        Transaction transaction = getTransactionSERVICE_PAYMENT_2();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 5 Оплата услуг от Клиента 2, сумма 1000",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT_2();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 6 Перевод на счет от Клиента 2, сумма 1000",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 7 Оплата услуг от Клиента 1, сумма 2000",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(new BigDecimal("2000.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 8 \"Перевод по номеру телефона\" от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM_GR_25);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "transaction8"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(new BigDecimal("1000.00"));
        return transaction;
    }

    private Transaction getTransactionSERVICE_PAYMENT_2() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(new BigDecimal("1000.00"));
        return transaction;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal("1000.00"));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(new BigDecimal("1000.00"));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("1000.00"));
        return transaction;
    }
}
