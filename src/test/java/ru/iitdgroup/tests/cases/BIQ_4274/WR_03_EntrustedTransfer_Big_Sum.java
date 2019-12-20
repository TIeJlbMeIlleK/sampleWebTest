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

public class WR_03_EntrustedTransfer_Big_Sum extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило WR_03 не срабатывает для непроверяемых типов транзакций и при отсутствии доверенного получателя по транзакции" + "ТК №3(87)");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:","5000")
                .fillInputText("Период серии в минутах:","5")
                .fillInputText("Статистический параметр Обнуления (0.95):","0,95")
                .save()
                .sleep(5);

        Table.Formula rows = getIC().locateTable("(Rule_tables) Разрешенные получатели Номер Телефона").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
    }

    @Test(
            description = "Генерация клиентов",
            dependsOnMethods = "enableRules"
    )
    public void client() {
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
            description = "Генерация клиентов",
            dependsOnMethods = "client"
    )
    public void GoodPayeeForClient() {
        Table.Formula rows1 = getIC().locateTable("(Rule_tables) Доверенные получатели").findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора  с сервис провайдером:","9299925912")
                .fillInputText("Наименование сервиса:","QIWI")
                .fillInputText("Наименование провайдера сервис услуги:", "QIWI")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(1))
                .fillInputText("Номер карты получателя:","4378723741117100")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(2))
                .fillInputText("Номер банковского счета получателя:","4081710835620000555")
                .fillInputText("БИК банка получателя:","042301754")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(3))
                .fillInputText("Наименование системы денежных переводов:","QIWI")
                .fillInputText("Наименование получателя в системе денежных переводов:","Нано Василий Петрович")
                .fillInputText("Страна получателя в системе денежных переводов:","Россия")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(4))
                .fillInputText("Наименование системы денежных переводов:","QIWI")
                .fillInputText("Наименование получателя в системе денежных переводов:","Иванов Иван Иванович")
                .fillInputText("Страна получателя в системе денежных переводов:","Россия")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(5))
                .fillInputText("Номер карты получателя:","4081710835620000741")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(6))
                .fillInputText("Номер карты получателя:","4081710835620000444")
                .save();


    }

    @Test(
            description = "Провести транзакцию № 1 \"Оплата услуг\"",
            dependsOnMethods = "GoodPayeeForClient"
    )
    public void transaction1() {
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getServicePayment()
                .setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setId("ACCOUNT");
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setName("Номер телефона");
        transactionData.getServicePayment().withProviderName("QIWI");
        transactionData.getServicePayment().withServiceName("QIWI");
        sendAndAssert(transaction);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Првоести транзакцию № 2 \"Перевод на карту\"",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getCardTransfer().withAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getCardTransfer().withDestinationCardNumber("4378723741117100");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на счет\"",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getOuterTransfer().withAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getOuterTransfer().getPayeeProps().setPayeeINN(null);
        transactionData.getOuterTransfer().getPayeeProps().setPayeeAccount("4081710835620000555");
        transactionData.getOuterTransfer().getPayeeBankProps().setBIK("042301754");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод через систему дененжных переводов\"",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getMTSystemTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getMTSystemTransfer().setReceiverName("Нано Василий Петрович");
        transactionData.getMTSystemTransfer().setMtSystem("QIWI");
        transactionData.getMTSystemTransfer().setReceiverCountry("Россия");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 5 \"Изменение перевода, отправленного через систему дененжных переводов\"",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransactionSDP_Refactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getMTTransferEdit().getSystemTransferCont().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getMTTransferEdit().getSystemTransferCont().setReceiverCountry("Россия");
        transactionData.getMTTransferEdit().getSystemTransferCont().setReceiverName("Иванов Иван Иванович");
        transactionData.getMTTransferEdit().getSystemTransferCont().setMtSystem("QIWI");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 6 \"Перевод по номеру телефона\" ",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getPhoneNumberTransfer().setDestinationCardNumber("4081710835620000741");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 7 \"Перевод по номеру телефона\" проверка на обнуление ",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(6));
        transactionData.withInitialSourceAmount(new BigDecimal(1000.00));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(951.00));
        transactionData.getPhoneNumberTransfer().setDestinationCardNumber("4081710835620000444");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
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
    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionSDP_Refactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
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
}
