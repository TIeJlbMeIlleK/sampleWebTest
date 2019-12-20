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


public class BR_01_PayeeInBlackList_Black_PhoneTransaction extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String BLACK_BIK_ACCOUNT = "(Rule_tables) Запрещенные получатели БИКСЧЕТ";
    private static final String BLACK_CARD = "(Rule_tables) Запрещенные получатели НомерКарты";
    private static final String BLACK_PHONE = "(Rule_tables) Запрещенные получатели НомерТелефона";
    private static final String BIK = "012345678";
    private static final String ACCOUNT = "40817810000000000002";
    private static final String CARD_NUMBER = "4712583155331208";
    private static final String PHONE_NUMBER = "9581551364";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило BR_01 работает с транзакцие \"Перевод по номеру телефона\"" + " ТК№45");


        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(3);
    }

    @Test(
            description = "Занести пару БИК+СЧЕТ в справочник запрещенных",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(BLACK_BIK_ACCOUNT).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(BLACK_BIK_ACCOUNT)
                .addRecord()
                .fillMasked("БИК:", BIK)
                .fillMasked("Счет:", ACCOUNT)
                .save();

        Table.Formula rows1 = getIC().locateTable(BLACK_CARD).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(BLACK_CARD)
                .addRecord()
                .fillInputText("НомерКарты:",CARD_NUMBER)
                .save();

        Table.Formula rows2 = getIC().locateTable(BLACK_PHONE).findRowsBy();
        if (rows2.calcMatchedRows().getTableRowNums().size() > 0) {
            rows2.delete();
        }
        getIC().locateTable(BLACK_PHONE)
                .addRecord()
                .fillInputText("Номер телефона:",PHONE_NUMBER)
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceData"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1 \"Перевод по номеру телефона\" на Номер телефона получателя  из справочника",
            dependsOnMethods = "client"
    )

    public void step1() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PHONE_NUMBER);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BLOCK_PHONE);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод по номеру телефона\" на Номер карты получателя  из справочника",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setDestinationCardNumber(CARD_NUMBER);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_CARD_IN_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод по номеру телефона\" на БИКСЧЕТ получателя  из справочника",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeeAccount(ACCOUNT);
        transactionData
                .getPhoneNumberTransfer()
                .setBIK(BIK);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_BIK_IN_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод по номеру телефона\"  получателя не из справочника",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
