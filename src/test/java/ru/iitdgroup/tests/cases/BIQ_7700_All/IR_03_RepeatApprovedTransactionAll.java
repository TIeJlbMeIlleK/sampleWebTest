package ru.iitdgroup.tests.cases.BIQ_7700_All;

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

public class IR_03_RepeatApprovedTransactionAll extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Людмила", "Серова", "Семеновна"}};

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", true)
                .fillInputText("Длина серии:", "2")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachIR03SelectAllType()
                .sleep(20);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод между счетами")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод в сторону государства")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Оплата услуг")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Открытие вклада")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Закрытие вклада")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Открытие счёта (в том числе накопительного)")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Закрытие счёта (в том числе накопительного)")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод с платежной карты стороннего банка на платежную карту РСХБ")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод через систему денежных переводов")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Подписка на сервисы оплаты")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос в госуслуги")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос реквизитов карты")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос CVC/CVV/CVP")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Покупка страховки держателей карт")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Отмена операции")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Изменение перевода, отправленного через систему денежных переводов")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
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
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод между счетами, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "addClients"
    )

    public void transBetween() {
        time.add(Calendar.HOUR, -10);
        Transaction transBetween = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetween = transBetween.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBetween
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetween
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBetween);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод между счетами», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenOutside = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenOutside = transBetweenOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBetweenOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetweenOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transBetweenOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenAccountBalance = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenAccountBalance = transBetweenAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBetweenAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetweenAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBetweenAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenDeviation = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenDeviation = transBetweenDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBetweenDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetweenDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transBetweenDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод между счетами» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenLength = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenLength = transBetweenLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBetweenLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetweenLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBetweenLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод на счет другому лицу, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transBetween"
    )

    public void transOuter() {
        time.add(Calendar.SECOND, 20);
        Transaction transOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transOuter.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuter
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuter
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuter);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод другому лицу», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterOutside = getOuterTransfer();
        TransactionDataType transactionDataOuterOutside = transOuterOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOuterOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBalance = getOuterTransfer();
        TransactionDataType transactionDataOuterAccountBalance = transOuterAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuterAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterDeviation = getOuterTransfer();
        TransactionDataType transactionDataOuterDeviation = transOuterDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод другому лицу» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterLength = getOuterTransfer();
        TransactionDataType transactionDataOuterLength = transOuterLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuterLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод в сторону государства, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transOuter"
    )

    public void transBudget() {
        time.add(Calendar.SECOND, 20);
        Transaction transBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transBudget.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBudget
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudget
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBudget);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод в сторону государства», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetOutside = getBudgetTransfer();
        TransactionDataType transactionDataBudgetOutside = transBudgetOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBudgetOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudgetOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transBudgetOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetAccountBalance = getBudgetTransfer();
        TransactionDataType transactionDataBudgetAccountBalance = transBudgetAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBudgetAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudgetAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBudgetAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetDeviation = getBudgetTransfer();
        TransactionDataType transactionDataBudgetDeviation = transBudgetDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBudgetDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudgetDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transBudgetDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод в сторону государства» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetLength = getBudgetTransfer();
        TransactionDataType transactionDataBudgetLength = transBudgetLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBudgetLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudgetLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transBudgetLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Оплата услуг, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transBudget"
    )

    public void transServis() {
        time.add(Calendar.SECOND, 20);
        Transaction transServis = getServisPayment();
        TransactionDataType transactionDataServis = transServis.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataServis
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServis
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transServis);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Оплата услуг», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServisOutside = getServisPayment();
        TransactionDataType transactionDataServisOutside = transServisOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataServisOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServisOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transServisOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServisAccountBalance = getServisPayment();
        TransactionDataType transactionDataServisAccountBalance = transServisAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataServisAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServisAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transServisAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServisDeviation = getServisPayment();
        TransactionDataType transactionDataServisDeviation = transServisDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataServisDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServisDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transServisDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Оплата услуг» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transServisLength = getServisPayment();
        TransactionDataType transactionDataServisLength = transServisLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataServisLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServisLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transServisLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Открытие вклада, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transServis"
    )

    public void transOpenDeposit() {
        time.add(Calendar.SECOND, 20);
        Transaction transOpenDeposit = getOpenDeposit();
        TransactionDataType transactionDataOpenDeposit = transOpenDeposit.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenDeposit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDeposit
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие вклада», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenDepositOutside = getOpenDeposit();
        TransactionDataType transactionDataOpenDepositOutside = transOpenDepositOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenDepositOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDepositOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOpenDepositOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenDepositAccountBalance = getOpenDeposit();
        TransactionDataType transactionDataOpenDepositAccountBalance = transOpenDepositAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenDepositAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDepositAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenDepositAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenDepositDeviation = getOpenDeposit();
        TransactionDataType transactionDataOpenDepositDeviation = transOpenDepositDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenDepositDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDepositDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOpenDepositDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Открытие вклада» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenDepositLength = getOpenDeposit();
        TransactionDataType transactionDataOpenDepositLength = transOpenDepositLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenDepositLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDepositLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenDepositLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие вклада» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Закрытие вклада, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transOpenDeposit"
    )

    public void transClosureDeposit() {
        time.add(Calendar.SECOND, 20);
        Transaction transClosureDeposit = getClosureDeposit();
        TransactionDataType transactionDataClosureDeposit = transClosureDeposit.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureDeposit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDeposit
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие вклада», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositOutside = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositOutside = transClosureDepositOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureDepositOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDepositOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transClosureDepositOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositAccountBalance = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositAccountBalance = transClosureDepositAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureDepositAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDepositAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureDepositAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositDeviation = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositDeviation = transClosureDepositDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureDepositDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDepositDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transClosureDepositDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Закрытие вклада» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositLength = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositLength = transClosureDepositLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureDepositLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDepositLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureDepositLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Открытие счёта (в том числе накопительного), проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transClosureDeposit"
    )

    public void transOpenAccount() {
        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccount = getOpenAccount();
        TransactionDataType transactionDataOpenAccount = transOpenAccount.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenAccount
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccount
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие счёта (в том числе накопительного)», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountOutside = getOpenAccount();
        TransactionDataType transactionDataOpenAccountOutside = transOpenAccountOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenAccountOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccountOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOpenAccountOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountAccountBalance = getOpenAccount();
        TransactionDataType transactionDataOpenAccountAccountBalance = transOpenAccountAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenAccountAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccountAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenAccountAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountDeviation = getOpenAccount();
        TransactionDataType transactionDataOpenAccountDeviation = transOpenAccountDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenAccountDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccountDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOpenAccountDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Открытие счёта (в том числе накопительного)» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountLength = getOpenAccount();
        TransactionDataType transactionDataOpenAccountLength = transOpenAccountLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOpenAccountLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccountLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOpenAccountLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Закрытие счёта (в том числе накопительного), проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transOpenAccount"
    )

    public void transClosureAccount() {
        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccount = getClosureAccount();
        TransactionDataType transactionDataClosureAccount = transClosureAccount.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureAccount
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccount
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие счёта (в том числе накопительного)», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountOutside = getClosureAccount();
        TransactionDataType transactionDataClosureAccountOutside = transClosureAccountOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureAccountOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccountOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transClosureAccountOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountBalance = getClosureAccount();
        TransactionDataType transactionDataClosureAccountBalance = transClosureAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountDeviation = getClosureAccount();
        TransactionDataType transactionDataClosureAccountDeviation = transClosureAccountDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureAccountDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccountDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transClosureAccountDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Закрытие счёта (в том числе накопительного)» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountLength = getClosureAccount();
        TransactionDataType transactionDataClosureAccountLength = transClosureAccountLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataClosureAccountLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccountLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transClosureAccountLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод с платежной карты стороннего банка на платежную карту РСХБ, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transClosureAccount"
    )

    public void transOuterCard() {
        time.add(Calendar.SECOND, 20);
        Transaction transOuterCard = getOuterCardTrans();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterCard
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCard
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCardOutside = getOuterCardTrans();
        TransactionDataType transactionDataOuterCardOutside = transOuterCardOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterCardOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCardOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOuterCardOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCardAccountBalance = getOuterCardTrans();
        TransactionDataType transactionDataOuterCardAccountBalance = transOuterCardAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterCardAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCardAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuterCardAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCardDeviation = getOuterCardTrans();
        TransactionDataType transactionDataOuterCardDeviation = transOuterCardDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterCardDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCardDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterCardDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод с платежной карты стороннего банка на платежную карту РСХБ» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCardLength = getOuterCardTrans();
        TransactionDataType transactionDataOuterCardLength = transOuterCardLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataOuterCardLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCardLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transOuterCardLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод через систему денежных переводов, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transOuterCard"
    )

    public void transMTSystem() {
        time.add(Calendar.SECOND, 20);
        Transaction transMTSystem = getMTSystemTrans();
        TransactionDataType transactionDataMTSystem = transMTSystem.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTSystem
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystem
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transMTSystem);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод через систему денежных переводов», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTSystemOutside = getMTSystemTrans();
        TransactionDataType transactionDataMTSystemOutside = transMTSystemOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTSystemOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystemOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transMTSystemOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTSystemAccountBalance = getMTSystemTrans();
        TransactionDataType transactionDataMTSystemAccountBalance = transMTSystemAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTSystemAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystemAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transMTSystemAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTSystemDeviation = getMTSystemTrans();
        TransactionDataType transactionDataMTSystemDeviation = transMTSystemDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTSystemDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystemDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transMTSystemDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод через систему денежных переводов» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transMTSystemLength = getMTSystemTrans();
        TransactionDataType transactionDataMTSystemLength = transMTSystemLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTSystemLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystemLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transMTSystemLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод через систему денежных переводов» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Подписка на сервисы оплаты, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transMTSystem"
    )

    public void transCreatSubscription() {
        time.add(Calendar.SECOND, 20);
        Transaction transCreatSubscription = getCreatSubscription();
        TransactionDataType transactionDataCreat = transCreatSubscription.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCreat
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreat
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transCreatSubscription);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Подписка на сервисы оплаты», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreatSubscriptionOutside = getCreatSubscription();
        TransactionDataType transactionDataCreatOutside = transCreatSubscriptionOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCreatOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreatOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(800.00));
        sendAndAssert(transCreatSubscriptionOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Подписка на сервисы оплаты» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreatSubscriptionAccountBalance = getCreatSubscription();
        TransactionDataType transactionDataCreatAccountBalance = transCreatSubscriptionAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCreatAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreatAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transCreatSubscriptionAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Подписка на сервисы оплаты» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreatSubscriptionDeviation = getCreatSubscription();
        TransactionDataType transactionDataCreatDeviation = transCreatSubscriptionDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCreatDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreatDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(372.25));
        sendAndAssert(transCreatSubscriptionDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Подписка на сервисы оплаты» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transCreatSubscriptionLength = getCreatSubscription();
        TransactionDataType transactionDataCreatLength = transCreatSubscriptionLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCreatLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreatLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transCreatSubscriptionLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Подписка на сервисы оплаты» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Запрос в госуслуги, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transCreatSubscription"
    )

    public void transRequestGosuslugi() {
        time.add(Calendar.SECOND, 20);
        Transaction transRequestGosuslugi = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugi = transRequestGosuslugi.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestGosuslugi
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestGosuslugiAccountBalance = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugiAccountBalance = transRequestGosuslugiAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestGosuslugiAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestGosuslugiAccountBalance
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(987);
        sendAndAssert(transRequestGosuslugiAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestGosuslugiDouble = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugiDouble = transRequestGosuslugiDouble.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestGosuslugiDouble
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestGosuslugiDouble);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос в госуслуги» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestGosuslugiLength = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugiLength = transRequestGosuslugiLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestGosuslugiLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestGosuslugiLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transRequestGosuslugiPeriod = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugiPeriod = transRequestGosuslugiPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestGosuslugiPeriod
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestGosuslugiPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Запрос на выдачу кредита, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transRequestGosuslugi"
    )

    public void transGettingCredit() {
        time.add(Calendar.SECOND, 20);
        Transaction transGettingCredit = getGettingCredit();
        TransactionDataType transactionDataGettingCredit = transGettingCredit.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataGettingCredit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCredit
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transGettingCredit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос на выдачу кредита», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transGettingCreditOutside = getGettingCredit();
        TransactionDataType transactionDataGettingCreditOutside = transGettingCreditOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataGettingCreditOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCreditOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transGettingCreditOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transGettingCreditAccountBalance = getGettingCredit();
        TransactionDataType transactionDataGettingCreditAccountBalance = transGettingCreditAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataGettingCreditAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCreditAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transGettingCreditAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transGettingCreditDeviation = getGettingCredit();
        TransactionDataType transactionDataGettingCreditDeviation = transGettingCreditDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataGettingCreditDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCreditDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transGettingCreditDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос на выдачу кредита» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transGettingCreditLength = getGettingCredit();
        TransactionDataType transactionDataGettingCreditLength = transGettingCreditLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataGettingCreditLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCreditLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transGettingCreditLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Запрос реквизитов карты, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transGettingCredit"
    )

    public void transRequestPAN() {
        time.add(Calendar.HOUR, -9);
        Transaction transRequestPAN = getRequestPAN();
        TransactionDataType transactionDataRequestPAN = transRequestPAN.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestPAN
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestPAN);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос реквизитов карты», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestPANAccountBalance = getRequestPAN();
        TransactionDataType transactionDataRequestPANAccountBalance = transRequestPANAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestPANAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestPANAccountBalance
                .getRequestPAN()
                .withSourceCardNumber("4275352520011118989");
        sendAndAssert(transRequestPANAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос реквизитов карты» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestPANDouble = getRequestPAN();
        TransactionDataType transactionDataRequestPANDouble = transRequestPANDouble.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestPANDouble
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestPANDouble);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос реквизитов карты» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestPANLength = getRequestPAN();
        TransactionDataType transactionDataRequestPANLength = transRequestPANLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestPANLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestPANLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос реквизитов карты» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transRequestPANPeriod = getRequestPAN();
        TransactionDataType transactionDataRequestPANPeriod = transRequestPANPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestPANPeriod
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestPANPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос реквизитов карты», условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Запрос CVC/CVV/CVP, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transRequestPAN"
    )

    public void transRequestCCV() {
        time.add(Calendar.HOUR, -18);
        Transaction transRequestCCV = getRequestCCV();
        TransactionDataType transactionDataRequestCCV = transRequestCCV.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestCCV
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestCCV);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос CVC/CVV/CVP», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVAccountBalance = getRequestCCV();
        TransactionDataType transactionDataRequestCCVAccountBalance = transRequestCCVAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestCCVAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestCCVAccountBalance
                .getRequestCCV()
                .withSourceCardNumber("4275388840011117777");
        sendAndAssert(transRequestCCVAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос CVC/CVV/CVP» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVDouble = getRequestCCV();
        TransactionDataType transactionDataRequestCCVDouble = transRequestCCVDouble.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestCCVDouble
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestCCVDouble);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос CVC/CVV/CVP» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVLength = getRequestCCV();
        TransactionDataType transactionDataRequestCCVLength = transRequestCCVLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestCCVLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestCCVLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос CVC/CVV/CVP» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transRequestCCVPeriod = getRequestCCV();
        TransactionDataType transactionDataRequestCCVPeriod = transRequestCCVPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataRequestCCVPeriod
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transRequestCCVPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос CVC/CVV/CVP», условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Отмена операции, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transRequestCCV"
    )

    public void transCancellation() {
        time.add(Calendar.SECOND, 20);
        Transaction transCancellation = getCancellationTrans();
        TransactionDataType transactionDataCancellation = transCancellation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCancellation
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transCancellation);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Отмена операции», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationAccountBalance = getCancellationTrans();
        TransactionDataType transactionDataCancellationAccountBalance = transCancellationAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCancellationAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCancellationAccountBalance
                .getTransactionCancellation()
                .withTransactionIdToCancel(444L);
        sendAndAssert(transCancellationAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Отмена операции» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationDouble = getCancellationTrans();
        TransactionDataType transactionDataCancellationDouble = transCancellationDouble.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCancellationDouble
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transCancellationDouble);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Отмена операции» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationLength = getCancellationTrans();
        TransactionDataType transactionDataCancellationLength = transCancellationLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCancellationLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transCancellationLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Отмена операции» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transCancellationPeriod = getCancellationTrans();
        TransactionDataType transactionDataCancellationPeripd = transCancellationPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCancellationPeripd
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transCancellationPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Отмена операции», условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Изменение перевода, отправленного через систему денежных переводов, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transCancellation"
    )

    public void transEditMT() {
        time.add(Calendar.SECOND, 20);
        Transaction transEditMT = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEdit = transEditMT.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTtransferEdit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEdit
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transEditMT);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Изменение перевода, отправленного через систему денежных переводов», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transEditMTOutside = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEditOutside = transEditMTOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTtransferEditOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEditOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transEditMTOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transEditMTAccountBalance = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEditAccountBalance = transEditMTAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTtransferEditAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEditAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transEditMTAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transEditMTDeviation = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEditDeviation = transEditMTDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTtransferEditDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEditDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transEditMTDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Изменение перевода, отправленного через систему денежных переводов» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transEditMTLength = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEditLength = transEditMTLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataMTtransferEditLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEditLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transEditMTLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Транзакция ЖКХ, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transEditMT"
    )

    public void transCommunal() {
        time.add(Calendar.SECOND, 20);
        Transaction transCommunal = getCommunalPayment();
        TransactionDataType transactionDataCommunal = transCommunal.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCommunal
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunal
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transCommunal);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Транзакция ЖКХ», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCommunalOutside = getCommunalPayment();
        TransactionDataType transactionDataCommunalOutside = transCommunalOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCommunalOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunalOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transCommunalOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCommunalAccountBalance = getCommunalPayment();
        TransactionDataType transactionDataCommunalAccountBalance = transCommunalAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCommunalAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunalAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transCommunalAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCommunalDeviation = getCommunalPayment();
        TransactionDataType transactionDataCommunalDeviation = transCommunalDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCommunalDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunalDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transCommunalDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Транзакция ЖКХ» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transCommunalLength = getCommunalPayment();
        TransactionDataType transactionDataCommunalLength = transCommunalLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataCommunalLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunalLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transCommunalLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Покупка страховки держателей карт, на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "transEditMT"
    )

    public void transBuyingInsurance() {
        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsurance = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsurance = transBuyingInsurance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBuyingInsurance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsurance
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transBuyingInsurance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Покупка страховки держателей карт», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceOutside = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceOutside = transBuyingInsuranceOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBuyingInsuranceOutside
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsuranceOutside
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(800.00));
        sendAndAssert(transBuyingInsuranceOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceAccountBalance = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceAccountBalance = transBuyingInsuranceAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBuyingInsuranceAccountBalance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsuranceAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transBuyingInsuranceAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceDeviation = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceDeviation = transBuyingInsuranceDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBuyingInsuranceDeviation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsuranceDeviation
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(372.25));
        sendAndAssert(transBuyingInsuranceDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Покупка страховки держателей карт» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceLength = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceLength = transBuyingInsuranceLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionDataBuyingInsuranceLength
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsuranceLength
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500.00));
        sendAndAssert(transBuyingInsuranceLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferBetweenAccounts() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getServisPayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getCreatSubscription() {
        Transaction transaction = getTransaction("testCases/Templates/CREATE_SUBSCRIPTION_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOpenDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_DEPOSIT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getClosureDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_DEPOSIT.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOpenAccount() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_ACCOUNT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getClosureAccount() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_ACCOUNT.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOuterCardTrans() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_CARD_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getMTSystemTrans() {
        Transaction transaction = getTransaction("testCases/Templates/MT_SYSTEM_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getRequestGosuslugi() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(325);
        return transaction;
    }

    private Transaction getGettingCredit() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getRequestPAN() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getRequestPAN()
                .withSourceCardNumber("4777344440099994444");
        return transaction;
    }

    private Transaction getRequestCCV() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getRequestCCV()
                .withSourceCardNumber("4275344440011112222");
        return transaction;
    }

    private Transaction getCancellationTrans() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSACTION_CANCELLATION.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getTransactionCancellation()
                .withTransactionIdToCancel(235L);
        return transaction;
    }

    private Transaction getMTTransferEdit() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getCommunalPayment() {
        Transaction transaction = getTransaction("testCases/Templates/COMMUNAL_PAYMENT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getBuyingInsurance() {
        Transaction transaction = getTransaction("testCases/Templates/BUYING_INSURANCE_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData()
                .getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
