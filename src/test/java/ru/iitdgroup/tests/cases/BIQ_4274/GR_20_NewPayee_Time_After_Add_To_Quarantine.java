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

public class GR_20_NewPayee_Time_After_Add_To_Quarantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String TABLE_GOOD = "(Rule_tables) Доверенные получатели";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String CARDNUMBER = "4378723743757560";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.DECEMBER, 5, 10, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    private static String this_time;
    private static String transactionID;


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило GR_20 срабатывает при наличии записи в \"Доверенные получатели\""+ " тк№3 -- BIQ4274");
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
            description = "Добавление в карантин получателей по клиенту",
            dependsOnMethods = "step0"
    )
    public void addToGoodPayee() {
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

        getIC().locateTable(TABLE_GOOD)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора  с сервис провайдером:","79299925912")
                .fillInputText("Наименование сервиса:","Mobile")
                .fillInputText("Наименование провайдера сервис услуги:","Beeline")
                .fillInputText("Номер карты получателя:",CARDNUMBER)
                .fillInputText("Наименование системы денежных переводов:","QIWI")
                .fillInputText("Наименование получателя в системе денежных переводов:","Парамонов Виктор Витальевич")
                .fillInputText("Страна получателя в системе денежных переводов:","РОССИЯ")
                .save();

    }

    @Test(
            description = "Провести транзакции",
            dependsOnMethods = "addToGoodPayee"
    )
    public void step1() {
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment()
                .setProviderName("Beeline");
        transactionData.getServicePayment()
                .setServiceName("Mobile");
        transactionData.getServicePayment()
                .getAdditionalField()
                .get(0)
                .setId("ACCOUNT");
        transactionData.getServicePayment()
                .getAdditionalField()
                .get(0)
                .setName("Номер телефона");
        transactionData.getServicePayment()
                .getAdditionalField()
                .get(0)
                .setValue("79299925912");
        this_time ="05.12.2019 10:00:00";


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Дата последней авторизованной транзакции:",this_time);
    }

    @Test(
            description = "Провести транзакции",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CARDNUMBER);
        this_time ="05.12.2019 10:05:00";

        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Дата последней авторизованной транзакции:",this_time);
    }

    @Test(
            description = "Провести транзакции",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getMTSystemTransfer()
                .withReceiverCountry("РОССИЯ")
                .withReceiverName("Парамонов Виктор Витальевич")
                .withMtSystem("QIWI");
        this_time ="05.12.2019 10:10:00";
        transactionID = transactionData.getTransactionId();

        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Дата последней авторизованной транзакции:",this_time);
    }

    @Test(
            description = "Провести транзакции",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionSdpRefactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getMTTransferEdit()
                .withEditingTransactionId(transactionID)
                .getSystemTransferCont()
                .setMtSystem("QIWI");
        transactionData.getMTTransferEdit()
                .getSystemTransferCont()
                .setReceiverName("Парамонов Виктор Витальевич");
        transactionData.getMTTransferEdit()
                .getSystemTransferCont()
                .setReceiverCountry("РОССИЯ");
        transactionData.getMTTransferEdit()
                .getSystemTransferCont()
                .setAmountInSourceCurrency(new BigDecimal(15000.00));
        this_time ="05.12.2019 10:15:00";

        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Дата последней авторизованной транзакции:",this_time);
    }

    @Test(
            description = "Провести транзакции",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("79299925912");
        this_time ="05.12.2019 10:20:00";

        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Дата последней авторизованной транзакции:",this_time);
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

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
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

    private Transaction getTransactionSdpRefactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
