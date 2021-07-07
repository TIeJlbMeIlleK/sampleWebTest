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

public class GR_03_SeriesOneToMany extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private static String id;
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Петр", "Зимушкин", "Федорович"}};

    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("Правило GR_03 срабатывает для транзакций в рамках BIQ-2296 -- BIQ2370 ТК№10(101)");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Сумма серии:", "1000")
                .fillCheckBox("Проверка регулярных:", false)
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
            description = "Провести транзакцию № 1 Перевод по номеру телефона для Клиента № 1, сумма 999",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(new BigDecimal("998.00"));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод через систему денежных переводов\" для Клиента № 1, сумма 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(new BigDecimal("2.00"));
        id = transaction.getData().getTransactionData().getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Изменение перевода через систему денежных переводов\" для Клиента № 1, сумма 1",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSdpRefactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(new BigDecimal("1.00"));
        transactionData
                .getMTTransferEdit()
                .withEditingTransactionId(id);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Провести транзакцию № 4, 5, 6 Перевод через систему денежных переводов для Клиента № 2, сумма 10",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(new BigDecimal("10.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Провести транзакцию № 4, 5, 6 Перевод через систему денежных переводов для Клиента № 2, сумма 10",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(new BigDecimal("10.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Провести транзакцию № 4, 5, 6 Перевод через систему денежных переводов для Клиента № 2, сумма 10",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(new BigDecimal("10.00"));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSdpRefactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
