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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_03_RepeatApprovedTransactionAmountDeviation extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String RULE_NAME1 = "R01_GR_20_NewPayee";
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
                .selectRule(RULE_NAME1)
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
                .attachTransactionIR03("Типы транзакций", "Платеж по QR-коду через СБП")
                .sleep(15);

//        getIC().locateTable(REFERENCE_TABLE)
//                .deleteAll()
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();
//        getIC().locateTable(REFERENCE_TABLE)
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();
//        getIC().locateTable(REFERENCE_TABLE)
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод по номеру телефона")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();
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
            description = "Отправить транзакцию №1 от клиента №1, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма - 500 руб, остаток - 10000 руб," +
                    "Перейти в Алерт - Подтвердить",
            dependsOnMethods = "addClients"
    )

    public void transactionPHONE1() {
        time.add(Calendar.MINUTE, -20);
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
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 200р, реквизиты и остаток на счету совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionPHONE1"
    )

    public void transactionPHONE2() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500, остаток на счету 9000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionPHONE2"
    )

    public void transactionPHONE3() {
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionPHONE3"
    )

    public void transactionPHONE4() {
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
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionPHONE4"
    )

    public void transactionPHONE5() {
        time.add(Calendar.MINUTE, 12);
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

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма - 500 руб, остаток - 10000 руб," +
                    "Перейти в Алерт - Подтвердить",
            dependsOnMethods = "transactionPHONE5"
    )

    public void transactionQR1() {
        time.add(Calendar.MINUTE, -20);
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
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Платеж по QR-коду через СБП», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 200р, реквизиты и остаток на счету совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionQR1"
    )

    public void transactionQR2() {
        Transaction transaction = getTransactionQR();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Платеж по QR-коду через СБП» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 500, остаток на счету 9000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionQR2"
    )

    public void transactionQR3() {
        Transaction transaction = getTransactionQR();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9000))
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Платеж по QR-коду через СБП» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionQR3"
    )

    public void transactionQR4() {
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

        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Платеж по QR-коду через СБП» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionQR4"
    )

    public void transactionQR5() {
        time.add(Calendar.MINUTE, 12);
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

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Платеж по QR-коду через СБП», условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма - 500 руб, остаток - 10000 руб," +
                    "Перейти в Алерт - Подтвердить",
            dependsOnMethods = "transactionQR5"
    )

    public void transactionCard1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 200р, реквизиты и остаток на счету совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionCard1"
    )

    public void transactionCard2() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500, остаток на счету 9000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionCard2"
    )

    public void transactionCard3() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionCard3"
    )

    public void transactionCard4() {
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, остаток на счету 10000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactionCard4"
    )

    public void transactionCard5() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
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
