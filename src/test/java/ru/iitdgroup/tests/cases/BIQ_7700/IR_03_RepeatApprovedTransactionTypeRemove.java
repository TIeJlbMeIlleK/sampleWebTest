package ru.iitdgroup.tests.cases.BIQ_7700;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransactionTypeRemove extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String RULE_NAME_ALERT = "R01_GR_20_NewPayee";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private static String TRANSACTION_ID;

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Кузьма", "Хорошев", "Семенович"}};

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
                .fillInputText("Длина серии:", "5")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .attachTransactionIR03("Типы транзакций", "Платеж по QR-коду через СБП")
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
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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
            description = "Отправить транзакцию №1 от клиента №1, тип транзакции Перевод по номеру телефона," +
                    "сумма - 500 руб, остаток - 10000 руб" +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- выполнить Action - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции Перевод по номеру телефона, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Перейти в правило, убрать из списка Типы транзакции: PHONE_NUMBER_TRANSFER." +
                    "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции Перевод по номеру телефона, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {

        // getDatabase().deleteWhere("itx_rule_configuration_INCOMING_LIST_FOR_I03", "WHERE [transactionTypes_id] = 2");//удаляет тип

        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .attachTransactionIR03("Типы транзакций", "Платеж по QR-коду через СБП")
                .sleep(25);

        time.add(Calendar.MINUTE, 3);
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Test(
            description = "Перейти в правило, добавить в список Типы транзакции: PHONE_NUMBER_TRANSFER." +
                    "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transaction2"
    )

    public void transaction4() {
        //getDatabase().insertRows("itx_rule_configuration_INCOMING_LIST_FOR_I03", new String[]{"'R01_IR_03_RepeatApprovedTransaction', '2'"});//добавляет тип
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .sleep(25);

        time.add(Calendar.MINUTE, 4);
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции PHONE_NUMBER_TRANSFER, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №4. " +
                    "(Проверка на превышение длины серии)",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionPHONE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1, тип транзакции Перевод на карту другому лицу," +
                    "сумма - 500 руб, остаток - 10000 руб" +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- выполнить Action - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "transaction5"
    )

    public void transactionCard1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        TRANSACTION_ID = transactionData.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionCard1"
    )

    public void transactionCard2() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Перейти в правило, убрать из списка Типы транзакции: Перевод на карту другому лицу" +
                    "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionCard2"
    )

    public void transactionCard3() {

        //       getDatabase().deleteWhere("itx_rule_configuration_INCOMING_LIST_FOR_I03", "WHERE [transactionTypes_id] = 1");//удаляет тип
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .attachTransactionIR03("Типы транзакций", "Платеж по QR-коду через СБП")
                .sleep(25);

        time.add(Calendar.MINUTE, 3);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Test(
            description = "Перейти в правило, добавить в список Типы транзакции: Перевод на карту другому лицу" +
                    "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionCard3"
    )

    public void transactionCard4() {

 //       getDatabase().insertRows("itx_rule_configuration_INCOMING_LIST_FOR_I03", new String[]{"'R01_IR_03_RepeatApprovedTransaction', '1'"});//добавляет тип
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .sleep(25);

        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №4. " +
                    "(Проверка на превышение длины серии)",
            dependsOnMethods = "transactionCard4"
    )

    public void transactionCard5() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionCARD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1, тип транзакции Платеж по QR-коду через СБП," +
                    "сумма - 500 руб, остаток - 10000 руб" +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- выполнить Action - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "transactionCard5"
    )

    public void transactionQRCode() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionQR();
        sendAndAssert(transaction);
        TRANSACTION_ID = transaction.getData().getTransactionData().getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Платеж по QR-коду через СБП», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", TRANSACTION_ID);
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 спустя 2 мин от транзакции №1, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionQRCode"
    )

    public void transactionQRCode1() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionQR();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Платеж по QR-коду через СБП» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Перейти в правило, убрать из списка Типы транзакции: Платеж по QR-коду через СБП" +
                    "Отправить транзакцию №3 от клиента №1 спустя 3 мин от транзакции №1, " +
                    "тип транзакции Перевод на карту другому лицу, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionQRCode1"
    )

    public void transactionQRCodq2() {

   //     getDatabase().deleteWhere("itx_rule_configuration_INCOMING_LIST_FOR_I03", "WHERE [transactionTypes_id] = 3");//удаляет тип из БД
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .sleep(25);

        time.add(Calendar.MINUTE, 3);
        Transaction transaction = getTransactionQR();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Test(
            description = "Перейти в правило, добавить в список Типы транзакции: Платеж по QR-коду через СБП" +
                    "Отправить транзакцию №4 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "реквизиты и остаток на счету совпадают с транзакцией №1.",
            dependsOnMethods = "transactionQRCodq2"
    )

    public void transactionQRCode3() {

       // getDatabase().insertRows("itx_rule_configuration_INCOMING_LIST_FOR_I03", new String[]{"'R01_IR_03_RepeatApprovedTransaction', '3'"});//добавляет тип
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .attachTransactionIR03("Типы транзакций", "Платеж по QR-коду через СБП")
                .sleep(25);

        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionQR();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Платеж по QR-коду через СБП» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 спустя 12 мин от транзакции №4, " +
                    "тип транзакции Платеж по QR-коду через СБП, сумма 500р, реквизиты и остаток на счету совпадают с транзакцией №4. " +
                    "(Проверка на превышение длины серии)",
            dependsOnMethods = "transactionQRCode3"
    )

    public void transactionQRCode4() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionQR();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Платеж по QR-коду через СБП», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionQR() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }

    private Transaction getTransactionPHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }

    private Transaction getTransactionCARD() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}