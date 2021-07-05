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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransaction1 extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

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
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Покупка страховки держателей карт")
                .attachTransactionIR03("Типы транзакций", "Запрос в госуслуги")
                .attachTransactionIR03("Типы транзакций", "Запрос на выдачу кредита")
                .attachTransactionIR03("Типы транзакций", "Запрос реквизитов карты")
                .attachTransactionIR03("Типы транзакций", "Запрос CVC/CVV/CVP")
                .attachTransactionIR03("Типы транзакций", "Отмена операции")
                .attachTransactionIR03("Типы транзакций", "Изменение перевода, отправленного через систему денежных переводов")
                .attachTransactionIR03("Типы транзакций", "Подписка на сервисы оплаты")
                .attachTransactionIR03("Типы транзакций", "Транзакция ЖКХ")
                .sleep(10);

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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод между счетами," +
                    "Перевод на счет другому лицу," +
                    "Перевод в сторону государства," +
                    "Оплата услуг," +
                    "Подписка на сервисы оплаты," +
                    "Открытие вклада," +
                    "Закрытие вклада," +
                    "Открытие счёта (в том числе накопительного)," +
                    "Закрытие счёта (в том числе накопительного)," +
                    "Перевод с платежной карты стороннего банка на платежную карту РСХБ," +
                    "Перевод через систему денежных переводов," +
                    "Покупка страховки держателей карт," +
                    "Запрос в госуслуги," +
                    "Запрос на выдачу кредита," +
                    "Запрос реквизитов карты," +
                    "Запрос CVC/CVV/CVP" +
                    "Отмена операции" +
                    "Изменение перевода, отправленного через систему денежных переводов," +
                    "Транзакция ЖКХ",
            dependsOnMethods = "addClients"
    )

    public void transferAll() {

        Transaction transBuyingInsurance = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsurance = transBuyingInsurance.getData().getTransactionData();
        transactionDataBuyingInsurance
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500));
        sendAndAssert(transBuyingInsurance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Покупка страховки держателей карт», условия правила не выполнены");

        Transaction transCreatSubscription = getCreatSubscription();
        TransactionDataType transactionDataCreat = transCreatSubscription.getData().getTransactionData();
        transactionDataCreat
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(500));
        sendAndAssert(transCreatSubscription);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Подписка на сервисы оплаты», условия правила не выполнены");

        Transaction transRequestGosuslugi = getRequestGosuslugi();
        sendAndAssert(transRequestGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");

        Transaction transGettingCredit = getGettingCredit();
        TransactionDataType transactionDataGettingCredit = transGettingCredit.getData().getTransactionData();
        transactionDataGettingCredit
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transGettingCredit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос на выдачу кредита», условия правила не выполнены");

        Transaction transRequestPAN = getRequestPAN();
        sendAndAssert(transRequestPAN);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос реквизитов карты», условия правила не выполнены");

        Transaction transRequestCCV = getRequestCCV();
        sendAndAssert(transRequestCCV);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос CVC/CVV/CVP», условия правила не выполнены");

        Transaction transCancellation = getCancellationTrans();
        sendAndAssert(transCancellation);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Отмена операции», условия правила не выполнены");

        Transaction transEditMT = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEdit = transEditMT.getData().getTransactionData();
        transactionDataMTtransferEdit
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transEditMT);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Изменение перевода, отправленного через систему денежных переводов», условия правила не выполнены");

        Transaction transCommunal = getCommunalPayment();
        TransactionDataType transactionDataCommunal = transCommunal.getData().getTransactionData();
        transactionDataCommunal
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transCommunal);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Транзакция ЖКХ», условия правила не выполнены");

        Transaction transBetween = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetween = transBetween.getData().getTransactionData();
        transactionDataBetween
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transBetween);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transOuter.getData().getTransactionData();
        transactionDataOuter
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOuter);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transBudget.getData().getTransactionData();
        transactionDataBudget
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transBudget);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transServis = getServisPayment();
        TransactionDataType transactionDataServis = transServis.getData().getTransactionData();
        transactionDataServis
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transServis);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transOpenDeposit = getOpenDeposit();
        TransactionDataType transactionDataOpenDeposit = transOpenDeposit.getData().getTransactionData();
        transactionDataOpenDeposit
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOpenDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transClosureDeposit = getClosureDeposit();
        TransactionDataType transactionDataClosureDeposit = transClosureDeposit.getData().getTransactionData();
        transactionDataClosureDeposit
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transClosureDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transOpenAccount = getOpenAccount();
        TransactionDataType transactionDataOpenAccount = transOpenAccount.getData().getTransactionData();
        transactionDataOpenAccount
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOpenAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transClosureAccount = getClosureAccount();
        TransactionDataType transactionDataClosureAccount = transClosureAccount.getData().getTransactionData();
        transactionDataClosureAccount
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transClosureAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transOuterCard = getOuterCardTrans();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData();
        transactionDataOuterCard
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transMTSystem = getMTSystemTrans();
        TransactionDataType transactionDataMTSystem = transMTSystem.getData().getTransactionData();
        transactionDataMTSystem
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transMTSystem);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferBetweenAccounts() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getServisPayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getCreatSubscription() {
        Transaction transaction = getTransaction("testCases/Templates/CREATE_SUBSCRIPTION_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getOpenDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_DEPOSIT_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getClosureDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_DEPOSIT.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getOpenAccount() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_ACCOUNT_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getClosureAccount() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_ACCOUNT.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getOuterCardTrans() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_CARD_TRANSFER_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getMTSystemTrans() {
        Transaction transaction = getTransaction("testCases/Templates/MT_SYSTEM_TRANSFER_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getRequestGosuslugi() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getGettingCredit() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getRequestPAN() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getRequestCCV() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getCancellationTrans() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSACTION_CANCELLATION.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getMTTransferEdit() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getCommunalPayment() {
        Transaction transaction = getTransaction("testCases/Templates/COMMUNAL_PAYMENT_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getBuyingInsurance() {
        Transaction transaction = getTransaction("testCases/Templates/BUYING_INSURANCE_Android.xml");
        transaction.getData().getServerInfo()
                .withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionDataType
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
