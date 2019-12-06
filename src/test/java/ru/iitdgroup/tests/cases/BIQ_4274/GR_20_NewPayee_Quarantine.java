package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.dbdriver.Database;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_Quarantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_GOOD = "(Rule_tables) Доверенные получатели";
    private static final String LOCAL_TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";

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
// Чистим справочники доверенных получателей и Карантина получателей по клиентам
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            getIC().getDriver().findElementByCssSelector("div[align='center']").click();
            getIC().getDriver().findElementByXPath("//*[text()='Actions']").click();
            getIC().getDriver().findElementByXPath("//*[@id=\"qtip-1-content\"]/a").click();
            getIC().getDriver().findElementByXPath("/html/body/div[17]/div[3]/div/button[2]").click();
        }
        Table.Formula rows1 = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            getIC().getDriver().findElementByCssSelector("div[align='center']").click();
            getIC().getDriver().findElementByXPath("//*[text()='Actions']").click();
            getIC().getDriver().findElementByXPath("//*[@id=\"qtip-1-content\"]/a").click();
            getIC().getDriver().findElementByXPath("/html/body/div[17]/div[3]/div/button[2]").click();
        }

        getIC().locateTable(LOCAL_TABLE).findRowsBy().match("Код значения","TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 7; i++) {
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
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("79599925915");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакции № 2 Перевод на карту, сумма 10",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650551178965454");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакции № 3 Перевод на счет, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));

        transactionData.getPhoneNumberTransfer()
                .setBIK("042301514");
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount("4081710835650000700");
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакции № 4 Перевод в бюджет, сумма 10",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4670551178965500");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE,5);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4670551178965500");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step5"
    )
    public void datePlus2Days() {
        Map<String, Object> values = new HashMap<>();
        values.put("TIME_STAMP", Instant.now().minus(2, ChronoUnit.DAYS).toString());

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.QUARANTINE_LIST", values, "WHERE CARDNUMBER = 4670551178965500");
        } catch (Exception e) {
            e.printStackTrace();
        }

        getIC().locateTable(TABLE_GOOD)
                .addRecord()
                .fillInputText("Номер карты получателя:","4154551178964123")
                .fillUser("Клиент:",clientIds.get(4))
                .save();
//        TODO Требуется перезагрузка САФ и Игнайт
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "datePlus2Days"
    )
    public void step6() {
        time.add(Calendar.MINUTE,5);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4670551178965500");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step6"
    )
    public void step7() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4154551178964123");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step7"
    )
    public void step8() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));

        transactionData.getCardTransfer()
                .setDestinationCardNumber("4154551178964123");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step8"
    )
    public void step9() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4104551178964155");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("79559295901");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step9"
    )
    public void step10() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4104551178965500");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("79559295901");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_EXIST_QUARANTINE_LOCATION);

        getIC().close();
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
    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
