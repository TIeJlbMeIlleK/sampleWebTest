package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.ves.mock.VesMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RDAKParametrisation extends RSHBCaseTest {

    private static final String TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";
    public static final String TABLE_2 = "(System_parameters) Параметры TimeOut";
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка справочника Параметры обработки справочников и флагов"
    )
    public void editTables() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Час начала рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "9")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Час окончания рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "18")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Минута начала рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "30")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Минута окончания рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "30")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Кол-во рабочих дней на которые Банк имеет право приостановить исполнение транзакции клиента")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
    }

    @Test(
            description = "Настройка справочника Параметры обработки справочников и флагов",
            dependsOnMethods = "editTables"
    )
    public void editTimeOut() {
        getIC().locateTable(TABLE_2).addRecord()
                .fillCheckBox("Календарь:", true)
                .fillInputText("Наименование канала ДБО:","Интернет клиент")
                .fillInputText("Тип транзакции:","Оплата услуг")
                .fillInputText("Время (мс):","3600000");
    }

    @Test(
            description = "Включкние стороннего правила для создания Алерта по транзакции",
            dependsOnMethods = "editTimeOut"
    )
    public void enableRuleForAlert() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editTables"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Произвести транзакцию 1 Перевод на карту другому лицу от Клиента 1, сумма 500",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal("500.00"));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 2 Перевод на карту другому лицу от Клиента 1, сумма 500",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal("500.00"));

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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal("1000.00"));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 4 Оплата услуг от Клиента 2, сумма 500",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT_2();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal("500.00"));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Произвести транзакцию 5 Оплата услуг от Клиента 2, сумма 500",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time2.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT_2();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal("500.00"));

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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getOuterTransfer().setAmountInSourceCurrency(new BigDecimal("1000.00"));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionSERVICE_PAYMENT_2() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2));
        return transaction;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private static VesMock getVesMock() {
        return VesMock.create().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");
    }
}
