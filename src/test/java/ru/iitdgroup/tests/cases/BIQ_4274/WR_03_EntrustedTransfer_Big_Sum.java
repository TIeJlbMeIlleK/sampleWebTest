package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

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
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final static String Table_Trusted = "(Rule_tables) Доверенные получатели";
    private final static String TABLE_ALLWED = "(Rule_tables) Разрешенные получатели Номер Телефона";
    private final String payeePhone = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String destinationCardNumber = "42768926" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationCardNumber2 = "42768926" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationCardNumber3 = "42768926" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount = "42700000" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final static String provider = "Beeline";
    private final static String service = "Mobile";
    private final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило WR_03 не срабатывает для непроверяемых типов транзакций и при отсутствии доверенного получателя по транзакции" + "ТК №3(87)");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "5")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .sleep(15);

        getIC().locateTable(TABLE_ALLWED)
                .deleteAll();
    }

    @Test(
            description = "Генерация клиентов",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");
                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Заполнение справочника",
            dependsOnMethods = "client"
    )
    public void GoodPayeeForClient() {
        getIC().locateTable(Table_Trusted)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", payeePhone)
                .fillInputText("Наименование сервиса:", service)
                .fillInputText("Наименование провайдера сервис услуги:", provider)
                .fillInputText("Номер карты получателя:", destinationCardNumber)
                .fillInputText("Номер банковского счета получателя:", payeeAccount)
                .fillInputText("БИК банка получателя:", BIK)
                .fillInputText("Наименование системы денежных переводов:", "QIWI")
                .fillInputText("Наименование получателя в системе денежных переводов:", "Нано Василий Петрович")
                .fillInputText("Страна получателя в системе денежных переводов:", "Россия")
                .save();

        getIC().locateTable(Table_Trusted)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Наименование системы денежных переводов:", "QIWI")
                .fillInputText("Наименование получателя в системе денежных переводов:", "Иванов Иван Иванович")
                .fillInputText("Страна получателя в системе денежных переводов:", "Россия")
                .fillInputText("Номер карты получателя:", destinationCardNumber2)
                .save();

        getIC().locateTable(Table_Trusted)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Номер карты получателя:", destinationCardNumber3)
                .save();
    }

    @Test(
            description = "Провести транзакцию  'Перевод по номеру телефона' проверка на обнуление ",
            dependsOnMethods = "GoodPayeeForClient"
    )
    public void transaction() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(1000.00));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(951.00))
                .withDestinationCardNumber(destinationCardNumber3);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 1 'Оплата услуг'",
            dependsOnMethods = "transaction"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .withProviderName(provider)
                .withServiceName(service)
                .getAdditionalField()
                .get(0)
                .withId("ACCOUNT")
                .withName("Номер телефона")
                .withValue(payeePhone);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Првоести транзакцию № 2 'Перевод на карту'",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .withDestinationCardNumber(destinationCardNumber);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 3 'Перевод на счет'",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .getPayeeBankProps()
                .withBIK(BIK);
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeINN(null)
                .withPayeeAccount(payeeAccount);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 4 'Перевод через систему дененжных переводов'",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .withReceiverName("Нано Василий Петрович")
                .withMtSystem("QIWI")
                .withReceiverCountry("Россия");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 5 'Изменение перевода, отправленного через систему дененжных переводов'",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP_Refactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .withReceiverCountry("Россия")
                .withReceiverName("Иванов Иван Иванович")
                .withMtSystem("QIWI");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Провести транзакцию № 6 Перевод по номеру телефона",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001.00))
                .withDestinationCardNumber(destinationCardNumber2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSDP_Refactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
