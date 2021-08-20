package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_25_SeriesTransfersAndPayments_RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private static final String RULE_NAME_2 = "R01_GR_15_NonTypicalGeoPosition";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private final static Random rand = new Random();
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("'Проверка не включения в серию переводов  транзакций, классифицированных как «Правомочно» при РДАК' -- BIQ4274 и ТК№19");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_2)
                .activate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:","60")
                .fillInputText("Сумма оплаты услуг:","2000")
                .fillInputText("Сумма серии:","2000")
                .save()
                .sleep(25);
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
            description = "Произвести транзакцию 1 Перевод на счет от Клиента 1, сумма 2000",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2000));
        transactionData
                .getClientDevice()
                .getPC()
                .withIpAddress("121.152.13."+rand.nextInt(100));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);

        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:","Правомочно");
        assertTableField("Идентификатор клиента:",clientIds.get(0));
        getIC().locateRules()
                .selectRule(RULE_NAME_2)
                .deactivate()
                .sleep(25);
        getIC().close();
    }

    @Test(
            description = "Произвести транзакцию 2 Перевод по номеру телефона от Клиента 1, сумма 500",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);

    }

    @Test(
            description = "Произвести транзакцию 3 Перевод на карту другому лицу от Клиента 1, сумма 1499",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1499.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 4 Перевод на счет от Клиента 1, сумма 1",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Провести транзакцию 5 Оплата услуг от Клиента 1, сумма 1999",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1999.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }
    @Test(
            description = "Провести транзакцию 6 Оплата услуг от Клиента 1, сумма 1",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1.00));
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
