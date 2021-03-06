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

public class GR_01_AnomalTransfer extends RSHBCaseTest {

    private static final String transactionTypeServicePayment = "PaymentMaxAmountServicePaymentType";
    private static final String transactionTypeCardTransfer = "PaymentMaxAmountCardTransferType";
    private static final String transactionTypeOuterTransfer = "PaymentMaxAmountOuterTransferType";
    private static final String transactionTypeBudgetTransfer = "PaymentMaxAmountBudgetTransferType";
    private static final String transactionTypePhoneNumberTransfer = "PaymentMaxAmountPhoneNumberTransfer";
    private static final String RULE_NAME = "R01_GR_01_AnomalTransfer";
    private static final BigDecimal MAX_AMMOUNT = BigDecimal.valueOf(11);
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Петр", "Зимушкин", "Федорович"}};
    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    //TODO Создан и настроен инцидент для правила (для few_data score 5, иначе 30), Cutting Score 30, иначе создается Алерт
    // и не рассчитывается максимальная сумма транзакции

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Величина отклонения (пример 0.05):", "0,2")
                .save()
                .sleep(25);

        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 2; i++) {

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
            description = "Провести транзакции № 1 Оплата услуг, сумма 10",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(FEW_DATA, RESULT_EMPTY_MAXAMOUNTLIST);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, BigDecimal.valueOf(10.00));
    }

    @Test(
            description = "Провести транзакции № 2 Перевод на карту, сумма 10",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(FEW_DATA, RESULT_EMPTY_MAXAMOUNTLIST);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeCardTransfer, BigDecimal.valueOf(10.00));
    }

    @Test(
            description = "Провести транзакции № 3 Перевод на счет, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(FEW_DATA, RESULT_EMPTY_MAXAMOUNTLIST);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeOuterTransfer, BigDecimal.valueOf(10.00));
    }

    @Test(
            description = "Провести транзакции № 4 Перевод в бюджет, сумма 10",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionBUDGET_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(FEW_DATA, RESULT_EMPTY_MAXAMOUNTLIST);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeBudgetTransfer, BigDecimal.valueOf(10.00));
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(11.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, BigDecimal.valueOf(11.00));
    }

    @Test(
            description = "Провести транзакцию № 6  Перевод на карту, сумма 11",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(11.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeCardTransfer, BigDecimal.valueOf(11.00));
    }

    @Test(
            description = "Провести транзакцию № 7 Перевод на счет, сумма 11",
            dependsOnMethods = "step6"
    )
    public void step7() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(11.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeOuterTransfer, BigDecimal.valueOf(11.00));
    }

    @Test(
            description = "Провести транзакцию № 7_Budget Перевод на счет, сумма 11",
            dependsOnMethods = "step7"
    )
    public void step7_Budget() {
        Transaction transaction = getTransactionBUDGET_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getBudgetTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(11.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeOuterTransfer, BigDecimal.valueOf(11.00));
    }

    @Test(
            description = "Провести транзакцию № 8 Оплата услуг, сумма 20",
            dependsOnMethods = "step7_Budget"
    )
    public void step8() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment()
                .setAmountInSourceCurrency(BigDecimal.valueOf(20.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, RESULT_ANOMAL_TRANSFER);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, MAX_AMMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 9 Перевод на карту, сумма 20",
            dependsOnMethods = "step8"
    )
    public void step9() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(20.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, RESULT_ANOMAL_TRANSFER);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, MAX_AMMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 10 Перевод на счет, сумма 20",
            dependsOnMethods = "step9"
    )
    public void step10() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(20.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, RESULT_ANOMAL_TRANSFER);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, MAX_AMMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 10 Перевод на счет, сумма 20",
            dependsOnMethods = "step10"
    )
    public void step10_Budget() {
        Transaction transaction = getTransactionBUDGET_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getBudgetTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(20.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, RESULT_ANOMAL_TRANSFER);
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeServicePayment, MAX_AMMOUNT);
    }

    @Test(
            description = "Провести транзакции № 11 Перевод между счетами, сумма 10",
            dependsOnMethods = "step10_Budget"
    )
    public void step11() {
        Transaction transaction = getTransactionBETWEEN_ACCOUNTS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getTransferBetweenAccounts()
                .setAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Провести \"Перевод по номеру телефона\" сумма 10",
            dependsOnMethods = "step11"
    )
    public void step12() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData()
                .getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(FEW_DATA, RESULT_EMPTY_MAXAMOUNTLIST);
        assertPaymentMaxAmount(clientIds.get(1), transactionTypePhoneNumberTransfer, BigDecimal.valueOf(10.00));
    }

    @Test(
            description = "Провести \"Перевод по номеру телефона\" сумма 11",
            dependsOnMethods = "step12"
    )
    public void step13() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData()
                .getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(11.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

        assertPaymentMaxAmount(clientIds.get(1), transactionTypePhoneNumberTransfer, BigDecimal.valueOf(11.00));
    }

    @Test(
            description = "Провести \"Перевод по номеру телефона\" сумма 20",
            dependsOnMethods = "step13"
    )
    public void step14() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData()
                .getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(20.00));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, RESULT_ANOMAL_TRANSFER);
        assertPaymentMaxAmount(clientIds.get(1), transactionTypePhoneNumberTransfer, MAX_AMMOUNT);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionBETWEEN_ACCOUNTS() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionBUDGET_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
