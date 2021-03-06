package ru.iitdgroup.tests.cases.BIQ_4274;

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

public class GR_07_BigTransfer extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_GR_07_BigTransfer";
    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};

    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("\"Правило GR_07 срабатывает только при соблюдении условий\" -- BIQ2370" + " ТК№13(103)");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Контроль лимита  (пример 0.05):", "0,1")
                .save()
                .sleep(30);
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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
                System.out.println(dboId);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакции № 1, 2 \"Перевод между счетами\", \"Перевод в бюджет\"",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionBUDGET();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Провести транзакции № 1, 2 \"Перевод между счетами\", \"Перевод в бюджет\"",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionBetweenAccounts();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Провести транзакции № 3, 4, 5 \"Перевод на карту\", \"Перевод на счет\"," +
                    " \"Перевод по номеру телефона\", сумма 9.01, лимит 10",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BIG_TRANSFER);
    }

    @Test(
            description = "Провести транзакции № 3, 4, 5 \"Перевод на карту\", \"Перевод на счет\"," +
                    " \"Перевод по номеру телефона\", сумма 9.01, лимит 10",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BIG_TRANSFER);
    }

    @Test(
            description = "Провести транзакции № 3, 4, 5 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 9.01, лимит 10",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BIG_TRANSFER);
    }

    @Test(
            description = "Провести транзакции № 6, 7, 8 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 8, лимит 10",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(new BigDecimal("8.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакции № 6, 7, 8 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 8, лимит 10",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal("8.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакции № 6, 7, 8 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 8, лимит 10",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("8.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакции № 9, 10, 11 \"Перевод на карту\", \"Перевод на счет\", " +
                    "\"Перевод по номеру телефона\", сумма 9.01, лимит не указан",
            dependsOnMethods = "transaction8"
    )
    public void transaction9() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withLimit(null);
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }

    @Test(
            description = "Провести транзакции № 9, 10, 11 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 9.01, лимит не указан",
            dependsOnMethods = "transaction9"
    )
    public void transaction10() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withLimit(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }

    @Test(
            description = "Провести транзакции № 9, 10, 11 \"Перевод на карту\", \"Перевод на счет\"," +
                    "\"Перевод по номеру телефона\", сумма 9.01, лимит не указан",
            dependsOnMethods = "transaction10"
    )
    public void transaction11() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData().withLimit(null);
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("9.01"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withLimit(BigDecimal.valueOf(10))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withConfirmationBigTransfer(null)
                .withRegular(false)
                .withVersion(1L)
                .withLimit(BigDecimal.valueOf(10))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withConfirmationBigTransfer(null)
                .withRegular(false)
                .withVersion(1L)
                .withLimit(BigDecimal.valueOf(10))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionBUDGET() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withLimit(BigDecimal.valueOf(10))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionBetweenAccounts() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withLimit(BigDecimal.valueOf(10))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }
}
