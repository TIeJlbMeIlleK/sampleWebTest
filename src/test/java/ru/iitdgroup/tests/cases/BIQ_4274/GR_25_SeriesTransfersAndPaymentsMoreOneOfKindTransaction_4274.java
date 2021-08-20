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

public class GR_25_SeriesTransfersAndPaymentsMoreOneOfKindTransaction_4274 extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("BIQ2370 и ТК№22 - 'Проверка соблюдения требования:  в серии есть не менее одной транзакции типа" +
                " «Перевод на счёт другому лицу» или «Перевод на карту другому лицу» и не менее одной транзакции «Оплата услуг»");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:","10")
                .fillInputText("Сумма оплаты услуг:","1000")
                .fillInputText("Сумма серии:","1000")
                .save()
                .sleep(20);
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
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
            description = "1. Произвести транзакцию 1 'Перевод на карту другому лицу' от Клиента 1, сумма 500",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "2. Произвести транзакцию 2 'Перевод по номеру телефона' от Клиента 1, сумма 500",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "3. Произвести транзакцию 3 'Оплата услуг' от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "4. Произвести транзакцию 4 'Оплата услуг' от Клиента 2, сумма 500",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "5. Произвести транзакцию 5 'Оплата услуг' от Клиента 2, сумма 500",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }
    @Test(
            description = "6. Произвести транзакцию 6 'Перевод на счет' от Клиента 2, сумма 1000",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "7. Произвести транзакцию 7 'Оплата услуг' от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        time.add(Calendar.MINUTE, -25);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 8 'Перевод по номеру телефона' от Клиента 1, сумма 500",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM_GR_25);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
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
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
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
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
