package ru.iitdgroup.tests.cases.BIQ_2370;

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

public class GR_25_SeriesTransfersAndPayments_WithRDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private static final String RULE_NAME_2 = "R01_GR_15_NonTypicalGeoPosition";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";

    private static Random rand = new Random();

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("\"Проверка не включения в серию переводов  транзакций, классифицированных как «Правомочно» при РДАК\" -- BIQ2370" + " ТК№7");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:","60")
                .fillInputText("Сумма оплаты услуг:","2000")
                .fillInputText("Сумма серии:","2000")
                .fillCheckBox("Проверка регулярных:",false)
                .save()
                .sleep(5);
        getIC().locateRules()
                .editRule(RULE_NAME_2)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(
            description = "Настроить WF для попадания первой транзакции на РДАК",
            dependsOnMethods = "enableRules"
    )
    public void refactorWF(){

        getIC().locateWorkflows()
                .openRecord("Alert Workflow").openAction("Взять в работу для выполнения РДАК")
                .clearAllStates()
                .addFromState("На разбор")
                .addFromState("Ожидаю выполнения РДАК")
                .addToState("На выполнении РДАК")
                .save();




//        TODO возможно данная таблица будет заполнена ранее.
        Table.Formula rows = getIC().locateTable(RDAK).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","rdak_underfire")
                .fillInputText("Новый статус:","RDAK_Done").save();
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","Wait_RDAK")
                .fillInputText("Новый статус:","RDAK_Done").save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "refactorWF"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "Произвести транзакцию 1 Перевод на счет от Клиента 1, сумма 2000",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer().setAmountInSourceCurrency(new BigDecimal("2000.00"));
        transactionData.getClientDevice().getPC().setIpAddress("121.152.13."+rand.nextInt(100));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(3);
        assertTableField("Resolution:","Правомочно");
        assertTableField("Идентификатор клиента:",clientIds.get(0));
        getIC().locateRules()
                .editRule(RULE_NAME_2)
                .fillCheckBox("Active:", false)
                .save()
                .sleep(5);
        getIC().close();

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test(
            description = "Произвести транзакцию 2 Перевод на счет от Клиента 1, сумма 500",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer().setAmountInSourceCurrency(new BigDecimal("500.00"));
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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal("1499.00"));

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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer().setAmountInSourceCurrency(new BigDecimal("1.00"));

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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal("1999.00"));

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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal("1.00"));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM_GR_25);
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
}
