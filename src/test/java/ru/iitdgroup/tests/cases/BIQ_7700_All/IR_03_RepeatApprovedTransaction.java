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

public class IR_03_RepeatApprovedTransaction extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

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
                .attachTransactionIR03("Типы транзакций", "Перевод между счетами")
                .attachTransactionIR03("Типы транзакций", "Перевод в сторону государства")
                .attachTransactionIR03("Типы транзакций", "Оплата услуг")
                .attachTransactionIR03("Типы транзакций", "Перевод другому лицу")
                .attachTransactionIR03("Типы транзакций", "Открытие вклада")
                .attachTransactionIR03("Типы транзакций", "Закрытие вклада")
                .attachTransactionIR03("Типы транзакций", "Открытие счёта (в том числе накопительного)")
                .attachTransactionIR03("Типы транзакций", "Закрытие счёта (в том числе накопительного)")
                .attachTransactionIR03("Типы транзакций", "Перевод с платежной карты стороннего банка на платежную карту РСХБ")
                .attachTransactionIR03("Типы транзакций", "Перевод через систему денежных переводов")
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
        Transaction transBetween = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetween = transBetween.getData().getTransactionData()
                .withRegular(false);
        transactionDataBetween
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBetween
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transBetween);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод между счетами», условия правила не выполнены");

        Transaction transOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transOuter.getData().getTransactionData()
                .withRegular(false);
        transactionDataOuter
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuter
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOuter);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод другому лицу», условия правила не выполнены");

        Transaction transBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transBudget.getData().getTransactionData()
                .withRegular(false);
        transactionDataBudget
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBudget
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transBudget);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод в сторону государства», условия правила не выполнены");

        Transaction transServis = getServisPayment();
        TransactionDataType transactionDataServis = transServis.getData().getTransactionData()
                .withRegular(false);
        transactionDataServis
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataServis
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transServis);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Оплата услуг», условия правила не выполнены");

        Transaction transOpenDeposit = getOpenDeposit();
        TransactionDataType transactionDataOpenDeposit = transOpenDeposit.getData().getTransactionData()
                .withRegular(false);
        transactionDataOpenDeposit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenDeposit
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getOpenDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOpenDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие вклада», условия правила не выполнены");

        Transaction transClosureDeposit = getClosureDeposit();
        TransactionDataType transactionDataClosureDeposit = transClosureDeposit.getData().getTransactionData()
                .withRegular(false);
        transactionDataClosureDeposit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureDeposit
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transClosureDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие вклада», условия правила не выполнены");

        Transaction transOpenAccount = getOpenAccount();
        TransactionDataType transactionDataOpenAccount = transOpenAccount.getData().getTransactionData()
                .withRegular(false);
        transactionDataOpenAccount
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOpenAccount
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOpenAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие счёта (в том числе накопительного)», условия правила не выполнены");

        Transaction transClosureAccount = getClosureAccount();
        TransactionDataType transactionDataClosureAccount = transClosureAccount.getData().getTransactionData()
                .withRegular(false);
        transactionDataClosureAccount
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataClosureAccount
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transClosureAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие счёта (в том числе накопительного)», условия правила не выполнены");

        Transaction transOuterCard = getOuterCardTrans();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData()
                .withRegular(false);
        transactionDataOuterCard
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataOuterCard
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");

        Transaction transMTSystem = getMTSystemTrans();
        TransactionDataType transactionDataMTSystem = transMTSystem.getData().getTransactionData()
                .withRegular(false);
        transactionDataMTSystem
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTSystem
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transMTSystem);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод через систему денежных переводов», условия правила не выполнены");


        Transaction transCreatSubscription = getCreatSubscription();
        TransactionDataType transactionDataCreat = transCreatSubscription.getData().getTransactionData()
                .withRegular(false);
        transactionDataCreat
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCreat
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCreateSubscription()
                .getAccuredPayment()
                .withMaxAmount(BigDecimal.valueOf(500));
        sendAndAssert(transCreatSubscription);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");


        Transaction transRequestGosuslugi = getRequestGosuslugi();
        TransactionDataType transactionDataRequestGosuslugi = transRequestGosuslugi.getData().getTransactionData()
                .withRegular(false);
        transactionDataRequestGosuslugi
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestGosuslugi
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        sendAndAssert(transRequestGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transGettingCredit = getGettingCredit();
        TransactionDataType transactionDataGettingCredit = transGettingCredit.getData().getTransactionData()
                .withRegular(false);
        transactionDataGettingCredit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataGettingCredit
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transGettingCredit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transRequestPAN = getRequestPAN();
        TransactionDataType transactionDataRequestPAN = transRequestPAN.getData().getTransactionData()
                .withRegular(false);
        transactionDataRequestPAN
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestPAN
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        sendAndAssert(transRequestPAN);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transRequestCCV = getRequestCCV();
        TransactionDataType transactionDataRequestCCV = transRequestCCV.getData().getTransactionData()
                .withRegular(false);
        transactionDataRequestCCV
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataRequestCCV
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        sendAndAssert(transRequestCCV);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transCancellation = getCancellationTrans();
        TransactionDataType transactionDataCancellation = transCancellation.getData().getTransactionData()
                .withRegular(false);
        transactionDataCancellation
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCancellation
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        sendAndAssert(transCancellation);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transEditMT = getMTTransferEdit();
        TransactionDataType transactionDataMTtransferEdit = transEditMT.getData().getTransactionData()
                .withRegular(false);
        transactionDataMTtransferEdit
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataMTtransferEdit
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transEditMT);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transCommunal = getCommunalPayment();
        TransactionDataType transactionDataCommunal = transCommunal.getData().getTransactionData()
                .withRegular(false);
        transactionDataCommunal
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataCommunal
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getOuterTransfer()
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transCommunal);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transBuyingInsurance = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsurance = transBuyingInsurance.getData().getTransactionData()
                .withRegular(false);
        transactionDataBuyingInsurance
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataBuyingInsurance
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500));
        sendAndAssert(transBuyingInsurance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferBetweenAccounts() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getServisPayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getCreatSubscription() {
        Transaction transaction = getTransaction("testCases/Templates/CREATE_SUBSCRIPTION_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOpenDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_DEPOSIT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getClosureDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_DEPOSIT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOpenAccount() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_ACCOUNT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getClosureAccount() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_ACCOUNT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getOuterCardTrans() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_CARD_TRANSFER_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getMTSystemTrans() {
        Transaction transaction = getTransaction("testCases/Templates/MT_SYSTEM_TRANSFER_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getRequestGosuslugi() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getGettingCredit() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getRequestPAN() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getRequestCCV() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getCancellationTrans() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSACTION_CANCELLATION.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getMTTransferEdit() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getCommunalPayment() {
        Transaction transaction = getTransaction("testCases/Templates/COMMUNAL_PAYMENT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getBuyingInsurance() {
        Transaction transaction = getTransaction("testCases/Templates/BUYING_INSURANCE_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
