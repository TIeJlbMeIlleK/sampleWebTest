package ru.iitdgroup.tests.cases.BIQ_7700;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransactionPHONE extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String RULE_NAME_ALERT = "R01_GR_20_NewPayee";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private static String TRANSACTION_ID;

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_ALERT)
                .activate();
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .edit()
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", true)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .sleep(15);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод по номеру телефона")
                .select("Наименование канала:", "Мобильный банк")
                .save();
    }

    @Test(
            description = "Создание клиентов",
           dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = " Провести транзакцию №1  для клиента №1, " +
                    "тип транзакции PAYMENT_C2B , сумма - 300р, остаток - 10000р;" +
                    "(Проверка на не совпадение Типа транзакции с условиями правила)",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
        Transaction transaction = getTransactionQR();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1, тип транзакции PHONE_NUMBER_TRANSFER," +
                    "сумма - 500 руб, остаток - 10000 руб",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1, тип транзакции CARD_TRANSFER," +
                    "сумма - 1000 руб, остаток - 10000 руб",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №4 от клиента №1 спустя 2 мин от транзакции №2, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №2.",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 2 мин от транзакции №3," +
                    "тип транзакции CARD_TRANSFER, сумма 1000р, реквизиты и остаток на счету совпадают с транзакцией №3.",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №6 от клиента №1 спустя 5 мин от транзакции №2, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 372,25р, реквизиты и остаток на счету совпадают с транзакцией №2.",
            dependsOnMethods = "transaction5"
    )

    public void transaction6() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №7 от клиента №1 спустя 3 мин от транзакции №3, " +
                    "тип транзакции CARD_TRANSFER, сумма 744,50р, реквизиты и остаток на счету совпадают с транзакцией №3.",
            dependsOnMethods = "transaction6"
    )

    public void transaction7() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(744.5));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №8 от клиента №1 спустя 7 мин от транзакции №2," +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №2." +
                    "(Проверка на превышение длины серии)",
            dependsOnMethods = "transaction7"
    )

    public void transaction8() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №9 от клиента №1 спустя 9 мин от транзакции №3," +
                    "тип транзакции CARD_TRANSFER, сумма 1000р, реквизиты и остаток на счету совпадают с транзакцией №3." +
                    "(Проверка на превышение длины серии)",
            dependsOnMethods = "transaction8"
    )

    public void transaction9() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionQR() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionPHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionCARD() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
