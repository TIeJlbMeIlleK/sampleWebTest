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

public class GR_23_GrayBIN extends RSHBCaseTest {

    private static final String TABLE_GREY_BIN = "(Rule_tables) Подозрительные банки BIN";
    private static final String RULE_NAME = "R01_GR_23_GrayBIN";
    private static final String BIN = "465015";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillInputText("Максимально допустимая сумма:", "1000")
                .save();

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        Table.Formula rows = getIC().locateTable(TABLE_GREY_BIN).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

        getIC().locateTable(TABLE_GREY_BIN)
                .addRecord()
                .fillInputText("BIN:",BIN)
                .save();
        getIC().close();

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
            description = "Провести транзакцию № 1 \"Перевод по номеру телефона\" на подозрительный БИН, сумма 1000",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650153743757560");
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(1000.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAY_BANK);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод по номеру телефона\" не на подозрительный БИН",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData().withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4730723743757560");
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(1000.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_NOT_GREY_BIN);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод по номеру телефона\" на подозрительный БИН, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData().withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650103743757560");
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(10.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_MAX_SUM);
    }


    @Test(
            description = "Провести транзакцию № 4 \"Перевод по номеру телефона\" на подозрительный БИН, сумма 1000, периодический",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);

        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650103743757560");
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(1000.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_REGULAR);
    }

    @Test(
            description = "Провести транзакцию № 5 на уникальный номер телефона(без указания \"Номера карты\" и \"БИКСЧЕТ\"), для клиента № 2 \"Перевод по номеру телефона\"",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);

        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(null);
        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("79299925912");
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(500.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MISSING_CARD_NUMBER);
    }

    @Test(
            description = "Провести транзакцию № 6 уникальный БИКСЧЕТ(без указания \"Номера карты\" и \"Номера телефона\"), для клиента № 3 \"Перевод по номеру телефона\"",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);

        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(null);
        transactionData.getPhoneNumberTransfer()
                .setBIK("042301514");
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount("4081710835650000700");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);
        transactionData.getPhoneNumberTransfer()
                .setAmountInSourceCurrency(new BigDecimal(500.00));
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MISSING_CARD_NUMBER);
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
