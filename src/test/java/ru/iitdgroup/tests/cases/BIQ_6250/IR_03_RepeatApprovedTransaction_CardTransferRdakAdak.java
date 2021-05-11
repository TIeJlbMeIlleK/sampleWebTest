package ru.iitdgroup.tests.cases.BIQ_6250;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_03_RepeatApprovedTransaction_CardTransferRdakAdak extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String PAYEE_1 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);
    private static final String PAYEE_2 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);
    private static final String PAYEE_3 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);
    private static final String PAYEE_4 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);
    private static final String PAYEE_5 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);
    private static final String PAYEE_6 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 16);

    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Леонид", "Жуков", "Игоревич"}, {"Ксения", "Новикова", "Сергеевна"}, {"Илья", "Птичкин", "Олегович"},
            {"Евгений", "Крымов", "Александрович"}, {"Иван", "Сырков", "Витальевич"}, {"Петр", "Серебряков", "Иванович"}};

//  TODO  Перед выполнением ТК, требуется создать Action: 6. Создан ручной Action WF в транзакции SetResolutionContinue, на изменение:
//TODO-- Status = Complete и
//TODO-- Resolution = Continue
//TODO-- ExternalApi -- Update Transaction In Cache.
//TODO 7. Создан ручной Action WF в транзакции SetResolutionFRAUD, на изменение:
//TODO-- Status = Complete и
//TODO-- Resolution = FRAUD
//TODO-- ExternalApi -- Update Transaction In Cache.
//TODO 8. Создан ручной Action WF (rdakStat) для Транзакции где в FieldMapping:
//TODO-- Статус РДАК = Success
//TODO-- ExternalApi -- Update Transaction In Cache.
//TODO 9. Создан ручной Action WF (adakStat) для Транзакции где в FieldMapping:
//TODO-- Статус АДАК = Success
//TODO-- ExternalApi -- Update Transaction In Cache.

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void enableRule() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", true)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .sleep(15);

        Table.Formula rows = getIC().locateTable(REFERENCE_TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
                .select("Наименование канала:", "Мобильный банк")
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRule"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 6; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                String login = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                String loginHash = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(login)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(loginHash)
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
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("adak_success")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = " Отправить Транзакцию №3 в обработку от Клиента №2 -- Получатель №2, сумма 500" +
                    "-- выполнить rdakStat и SetResolutionContinue для транзакции",
            dependsOnMethods = "step2"
    )

    public void step3() {

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("РДАК выполнен:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .save().sleep(20);


        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_2);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("rdak_success")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку от Клиента №2 -- Получатель №2, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_2);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №5 в обработку, от Клиента №3 -- Получатель №3, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("РДАК выполнен:", true)
                .fillCheckBox("АДАК выполнен:", true)
                .save().sleep(15);

        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_3);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("rdak_success")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("adak_success")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №6 в обработку от Клиента №3 -- Получатель №3, сумма 500",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_3);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №7 в обработку, от Клиента №4 -- Получатель №4, сумма 500",
            dependsOnMethods = "step6"
    )

    public void step7() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_4);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("rdak_success")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("adak_success")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst().getActions()
                .doAction("Резолюция мошенничество")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №8 в обработку от Клиента №4 -- Получатель №4, сумма 500",
            dependsOnMethods = "step7"
    )

    public void step8() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_4);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №9 в обработку, от Клиента №5 -- Получатель №5, сумма 500",
            dependsOnMethods = "step8"
    )

    public void step9() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_5);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("rdak_success")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №10  в обработку от Клиента №5 -- Получатель №5, сумма 500",
            dependsOnMethods = "step9"
    )

    public void step10() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_5);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №11 в обработку, от Клиента №6 -- Получатель №6, сумма 500",
            dependsOnMethods = "step10"
    )

    public void step11() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(5));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_6);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("adak_success")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №12  в обработку от Клиента №6 -- Получатель №6, сумма 500",
            dependsOnMethods = "step11"
    )

    public void step12() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_5);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
