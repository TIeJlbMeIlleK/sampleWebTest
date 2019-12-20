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

public class WR_03_EntrustedTransfer_PeriodSeries extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time1 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time3 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time4 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time5 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time6 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final GregorianCalendar time7 = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
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
    }

    @Test(
            description = "Генерация клиентов",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 8; i++) {
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
            description = "Добавление доверенных получателей по клиентам",
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
                .fillUser("Клиент:",clientIds.get(7))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора  с сервис провайдером:","9515478963")
                .save();

        getIC().locateTable("(Rule_tables) Доверенные получатели")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(6))
                .fillInputText("Номер банковского счета получателя:","40817815000000000001")
                .fillInputText("БИК банка получателя:","015555678")
                .save();
    }

    @Test(
            description = "Провести транзакцию № 1 и 2 \"Оплата услуг\"",
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
                .setAmountInSourceCurrency(new BigDecimal(10.00));
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
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);


        time.add(Calendar.MINUTE, 1);
        Transaction transaction2 = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getServicePayment()
                .setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        transactionData2.getServicePayment()
                .getAdditionalField().get(0).setId("ACCOUNT");
        transactionData2.getServicePayment()
                .getAdditionalField().get(0).setName("Номер телефона");
        transactionData2.getServicePayment().withProviderName("QIWI");
        transactionData2.getServicePayment().withServiceName("QIWI");
        sendAndAssert(transaction2);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Првоести транзакцию № 3 и 4 \"Перевод на карту\"",
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
        transactionData.getCardTransfer().withAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getCardTransfer().withDestinationCardNumber("4378723741117100");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time2.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2.getClientIds().withDboId(clientIds.get(1));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getCardTransfer().withAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getCardTransfer().withDestinationCardNumber("4378723741117100");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Провести транзакцию № 4 и 6 \"Перевод на счет\"",
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
        transactionData.getOuterTransfer().withAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getOuterTransfer().getPayeeProps().setPayeeINN(null);
        transactionData.getOuterTransfer().getPayeeProps().setPayeeAccount("4081710835620000555");
        transactionData.getOuterTransfer().getPayeeBankProps().setBIK("042301754");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        time1.add(Calendar.MINUTE, 1);
        Transaction transaction2 = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2.getClientIds().withDboId(clientIds.get(2));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getOuterTransfer().withAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getOuterTransfer().getPayeeProps().setPayeeINN(null);
        transactionData2.getOuterTransfer().getPayeeProps().setPayeeAccount("4081710835620000555");
        transactionData2.getOuterTransfer().getPayeeBankProps().setBIK("042301754");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Провести транзакцию № 7 и 8 \"Перевод через систему дененжных переводов\"",
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
        transactionData.getMTSystemTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getMTSystemTransfer().setReceiverName("Нано Василий Петрович");
        transactionData.getMTSystemTransfer().setMtSystem("QIWI");
        transactionData.getMTSystemTransfer().setReceiverCountry("Россия");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time3.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionSDP();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2.getClientIds().withDboId(clientIds.get(3));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getMTSystemTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getMTSystemTransfer().setReceiverName("Нано Василий Петрович");
        transactionData2.getMTSystemTransfer().setMtSystem("QIWI");
        transactionData2.getMTSystemTransfer().setReceiverCountry("Россия");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Провести транзакцию № 9 и 10 \"Изменение перевода, отправленного через систему дененжных переводов\"",
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
        transactionData.getMTTransferEdit().getSystemTransferCont().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getMTTransferEdit().getSystemTransferCont().setReceiverCountry("Россия");
        transactionData.getMTTransferEdit().getSystemTransferCont().setReceiverName("Иванов Иван Иванович");
        transactionData.getMTTransferEdit().getSystemTransferCont().setMtSystem("QIWI");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time4.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionSDP_Refactor();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getMTTransferEdit().getSystemTransferCont().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getMTTransferEdit().getSystemTransferCont().setReceiverCountry("Россия");
        transactionData2.getMTTransferEdit().getSystemTransferCont().setReceiverName("Иванов Иван Иванович");
        transactionData2.getMTTransferEdit().getSystemTransferCont().setMtSystem("QIWI");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);


    }

    @Test(
            description = "Провести транзакцию № 11 и 12 \"Перевод по номеру телефона\" ",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_CARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getPhoneNumberTransfer().setDestinationCardNumber("4081710835620000741");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time5.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionPHONE_NUMBER_TRANSFER_WITH_CARD();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getPhoneNumberTransfer().setDestinationCardNumber("4081710835620000741");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Провести транзакцию № 13 и 14 \"Перевод по номеру телефона\"",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(7));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getPhoneNumberTransfer().setPayeePhone("9515478963");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time7.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(7));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getPhoneNumberTransfer().setPayeePhone("9515478963");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
    }

    @Test(
            description = "Провести транзакцию № 15 и 16 \"Перевод по номеру телефона\"",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER_WITH_BIK();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(6));
        transactionData.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData.getPhoneNumberTransfer().setBIK("015555678");
        transactionData.getPhoneNumberTransfer().setPayeeAccount("40817815000000000001");
        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);

        time6.add(Calendar.MINUTE, 1);

        Transaction transaction2 = getTransactionPHONE_NUMBER_TRANSFER_WITH_BIK();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(6));
        transactionData2.withInitialSourceAmount(new BigDecimal(10000.00));
        transactionData2.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(10.00));
        transactionData2.getPhoneNumberTransfer().setBIK("015555678");
        transactionData2.getPhoneNumberTransfer().setPayeeAccount("40817815000000000001");
        sendAndAssert(transaction2);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_PERIOD);
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
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1));
        return transaction;
    }
    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2));
        return transaction;
    }
    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time3))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time3));
        return transaction;
    }
    private Transaction getTransactionSDP_Refactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time4))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time4));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER_WITH_CARD() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time5))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time5));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER_WITH_BIK() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time6))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time6));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER_WITH_PHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time7))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time7));
        return transaction;
    }
}
