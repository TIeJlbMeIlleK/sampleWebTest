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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_04_ToVIP_WITH_VIP_PAYEE extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_04_ToVip";
    private static final String TABLE_VIP_CARD = "(Rule_tables) VIP клиенты НомерКарты";
    private static final String TABLE_VIP_BIK_ACCOUNT = "(Rule_tables) VIP клиенты БИКСЧЕТ";
    private static final String BIK = "012345901";
    private static final String ACCOUNT = "40817810000000000555";
    private static final String CARD_NUMBER = "4712583155776208";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило WR_04 не срабатывает для непроверяемых типов транзакций и при отсутствии VIP-получателя" + " ТК№7(91)");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        Table.Formula rows = getIC().locateTable(TABLE_VIP_CARD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        Table.Formula rows1 = getIC().locateTable(TABLE_VIP_BIK_ACCOUNT).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }

        getIC().locateTable(TABLE_VIP_CARD)
                .addRecord()
                .fillInputText("Номер Карты:",CARD_NUMBER)
                .save();

        getIC().locateTable(TABLE_VIP_BIK_ACCOUNT)
                .addRecord()
                .fillMasked("Бик банка VIP:",BIK)
                .fillMasked("Счет получатель VIP:",ACCOUNT)
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
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
            description = "Провести транзакцию \"Перевод на карту\"",
            dependsOnMethods = "client"
    )
    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CARD_NUMBER);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_VIP_LIST);
    }

    @Test(
            description = "Провести транзакцию \"Перевод на счет\"",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount(ACCOUNT);
        transactionData.getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(BIK);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_VIP_LIST);
    }

    @Test(
            description = "Провести транзакцию \"Перевод по номеру телефона\" --- номер карты указан в справочнике по НомерКарты",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(CARD_NUMBER);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_VIP_LIST);
    }

    @Test(
            description = "Провести транзакцию \"Перевод на карту\" для получателя, не находящегося в справочнике \"VIP клиенты НомерКарты\"",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setBIK(BIK);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(ACCOUNT);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_VIP_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
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
}
