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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class R01_GR_03_SeriesOneToMany_Regular extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("BIQ2370 и ТК№12(102) - Правило GR_03 срабатывает только при соблюдении условий с учетом регулярных транзакций");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Длина серии:","3")
                .fillInputText("Период серии в минутах:","10")
                .fillInputText("Сумма серии:","1000")
                .fillCheckBox("Проверка регулярных:", true)
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
            description = "Провести транзакцию № 1 \"Перевод по номеру телефона\"  для Клиента № 1, сумма 1, регулярная",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.HOUR, -3);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("1.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод на счет\" для Клиента № 1, сумма 998",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal("998.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на карту\" для Клиента № 1, сумма 2, регулярная",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(new BigDecimal(2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод по номеру телефона\" для Клиента № 2, сумма 1001, регулярная",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("1001.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод на счет\" для Клиента № 3, сумма 10, регулярные",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }
    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод на счет\" для Клиента № 3, сумма 10, регулярные",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 5, 6, 7 \"Перевод на счет\" для Клиента № 3, сумма 10, регулярные",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
    }

    @Test(
            description = "Провести транзакцию № 8, 9 \"Перевод на счет\" для Клиента № 4 сумма 10, регулярная",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 8, 9 \"Перевод на счет\" для Клиента № 4 сумма 10, регулярная",
            dependsOnMethods = "transaction8"
    )
    public void transaction9() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 10 'Перевод по номеру телефона' для Клиента № 4, сумма 10, " +
                    "спустя 11 минут после транзакции № 8, регулярная",
            dependsOnMethods = "transaction9"
    )
    public void transaction10() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 11 \"Перевод на счет\"  для Клиента № 5, сумма 1",
            dependsOnMethods = "transaction10"
    )
    public void transaction11() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 12 \"Перевод на счет\" для Клиента № 5, сумма 998, регулярная",
            dependsOnMethods = "transaction11"
    )
    public void transaction12() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(998.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 13 \"Перевод на карту\" для Клиента № 5, сумма 1",
            dependsOnMethods = "transaction12"
    )
    public void transaction13() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
    }

    @Test(
            description = "Провести транзакцию № 14 \"Перевод на счет\"  для Клиента № 6, сумма 501, регулярная ",
            dependsOnMethods = "transaction13"
    )
    public void transaction14() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(501.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
    }

    @Test(
            description = "Провести транзакцию № 15 \"Перевод на счет\" для Клиента № 6, сумма 500, регулярная",
            dependsOnMethods = "transaction14"
    )
    public void transaction15() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
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
}
