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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_Quarantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final BigDecimal MAX_AMMOUNT = BigDecimal.valueOf(11);

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .save();

//        TODO требуется реализовать настройку блока Alert Scoring Model по правилу + Alert Scoring Model общие настройки

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
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
            description = "Провести транзакции № 1 Оплата услуг, сумма 10",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber("4378723743757555");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Номер Карты получателя:","4378723743757555");
    }

    @Test(
            description = "Провести транзакции № 2 Перевод на карту, сумма 10",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getMTSystemTransfer()
                .withReceiverName("Иванов Иван Иванович")
                .setReceiverCountry("РОССИЯ");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
        Table.Formula rows1 = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows1.click();}
        assertTableField("Наименование получателя в системе денежных переводов:","Иванов Иван Иванович");
        assertTableField("Страна получателя в системе денежных переводов:","РОССИЯ");

    }

    @Test(
            description = "Провести транзакции № 3 Перевод на счет, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

        Transaction transaction = getTransactionSDP_REFACTOR();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getMTTransferEdit()
                .getSystemTransferCont().setReceiverName("Григорьев Николай Петрович");
        transactionData.getMTTransferEdit()
                .getSystemTransferCont().setReceiverCountry("США");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
        Table.Formula rows1 = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows1.click();}
        assertTableField("Наименование получателя в системе денежных переводов:","Григорьев Николай Петрович");
        assertTableField("Страна получателя в системе денежных переводов:","США");
    }

    @Test(
            description = "Провести транзакции № 4 Перевод в бюджет, сумма 10",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps().setPayeeAccount("4081710835620000888");
        transactionData.getOuterTransfer()
                .getPayeeProps().setPayeeINN("0987654321");
        transactionData.getOuterTransfer()
                .getPayeeProps().setPayeeName("Иванов Иван Иванович");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
        Table.Formula rows1 = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows1.click();}
        assertTableField("Имя получателя:","Иванов Иван Иванович");
        assertTableField("Счет получателя:","4081710835620000888");
        assertTableField("ИНН получателя:","0987654321");

    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step4"
    )
    public void step5() {
//        TODO ТРЕБУЕТСЯ РЕАЛИЗОВАТЬ ИЗМЕНЕНИЕ ПОЛЕЙ В REFERENCE TABLE
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

        Transaction transaction = getTransactionTELEPHON_VALUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
//        transactionData.getOuterTransfer()
//                .getPayeeProps().setPayeeAccount("4081710835620000888");
//        transactionData.getOuterTransfer()
//                .getPayeeProps().setPayeeINN("0987654321");
//        transactionData.getOuterTransfer()
//                .getPayeeProps().setPayeeName("Иванов Иван Иванович");

//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
//        Table.Formula rows1 = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
//        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows1.click();}
//        assertTableField("Имя получателя:","Иванов Иван Иванович");
//        assertTableField("Счет получателя:","4081710835620000888");
//        assertTableField("ИНН получателя:","0987654321");

    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
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
    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionSDP_REFACTOR() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionTELEPHON_VALUE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
