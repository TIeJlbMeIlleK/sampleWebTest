package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PaymentMaxAmountJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Александр", "Новиков", "Кузьминович"}};
    private static final String RULE_NAME = "R01_GR_01_AnomalTransfer";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";

    private static String transactionIdServ1;
    private static String transactionIdServ2;
    private static String transactionIdOuter;
    private static String transactionIdCard;
    private static String transactionIdBudget;
    private static String transactionIdPhone;
    private static final Long version1 = 1L;
    private static final Long version2 = 2L;

    private static final String transactionTypeServicePayment = "PaymentMaxAmountServicePaymentType";
    private static final String transactionTypeCardTransfer = "PaymentMaxAmountCardTransferType";
    private static final String transactionTypeOuterTransfer = "PaymentMaxAmountOuterTransferType";
    private static final String transactionTypeBudgetTransfer = "PaymentMaxAmountBudgetTransferType";
    private static final String transactionTypePhoneNumberTransfer = "PaymentMaxAmountPhoneNumberTransfer";

    //TODO Создан и настроен инцидент для правила (для few_data score 5, иначе 30), Cutting Score 30

    @Test(
            description = "Включить правило: Отклонение 0.2; остальные правила деактивированы"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Величина отклонения (пример 0.05):", "0,2")
                .save()
                .sleep(20);

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "PERIOD_STATISTIC_COLLECTION")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
    }

    @Test(
            description = "Создаем клиента",
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
            description = "Провести транзакции № 1, 1.1, 2, 3, 4, 5" +
                    "-- \"Оплата услуг\" (Version = 1, transactionID = 1), \"Оплата услуг\" (Version = 2, transactionID = 1),сумма 10" +
                    "-- \"Перевод на карту\" (Version = 1, transactionID = 2), сумма 10" +
                    "-- \"Перевод на счет\" (Version = 1, transactionID = 3), сумма 10" +
                    "-- \"Перевод в бюджет\" (Version = 1, transactionID = 4), сумма 10" +
                    "-- \"Перевод по номеру телефона\" (Version = 1, transactionID = 5), сумма 10",
            dependsOnMethods = "addClients"
    )

    public void firstTransactions() {
        Transaction transaction = getTransactionServicePayment();
        transactionIdServ1 = transaction.getData().getTransactionData().getTransactionId();
        sendAndAssert(transaction);

        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .withVersion(version2);
        transactionIdServ2 = transactionTwo.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionTwo);

        Transaction transactionCard = getCardTransfer();
        transactionIdCard = transactionCard.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionCard);

        Transaction transactionBudget = getBudgetTransfer();
        transactionIdBudget = transactionBudget.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionBudget);

        Transaction transactionPhone = getPhoneNumberTransfer();
        transactionIdPhone = transactionPhone.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionPhone);

        Transaction transactionOuter = getOuterTransfer();
        transactionIdOuter = transactionOuter.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionOuter);

        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdServ1, transactionTypeServicePayment, BigDecimal.valueOf(10));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version2.toString(), transactionIdServ2, transactionTypeServicePayment, BigDecimal.valueOf(10));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdBudget, transactionTypeBudgetTransfer, BigDecimal.valueOf(10));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdPhone, transactionTypePhoneNumberTransfer, BigDecimal.valueOf(10));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdOuter, transactionTypeOuterTransfer, BigDecimal.valueOf(10));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdCard, transactionTypeCardTransfer, BigDecimal.valueOf(10));
    }

    @Test(
            description = "Провести транзакцию № 6, 6.1, 7, 8, 9, 10" +
                    "-- \"Оплата услуг\" (Version = 1, transactionID = 6), \"Оплата услуг\" (Version = 2, transactionID = 2), сумма 12" +
                    "-- \"Перевод на карту\" (Version = 1, transactionID = 7), сумма 12" +
                    "-- \"Перевод на счет\" (Version = 1, transactionID = 8), сумма 12" +
                    "-- \"Перевод в бюджет\" (Version = 1, transactionID = 9),сумма 12" +
                    "-- \"Перевод по номеру телефона\" (Version = 1, transactionID = 10),  сумма 12",
            dependsOnMethods = "firstTransactions"
    )

    public void secondTransactions() {
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdServ1 = transaction.getData().getTransactionData().getTransactionId();
        sendAndAssert(transaction);

        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .withVersion(version2)
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdServ2 = transactionTwo.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionTwo);

        Transaction transactionCard = getCardTransfer();
        TransactionDataType transactionDataCard = transactionCard.getData().getTransactionData();
        transactionDataCard
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdCard = transactionCard.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionCard);

        Transaction transactionBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transactionBudget.getData().getTransactionData();
        transactionDataBudget
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdBudget = transactionBudget.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionBudget);

        Transaction transactionPhone = getPhoneNumberTransfer();
        TransactionDataType transactionDataPhone = transactionPhone.getData().getTransactionData();
        transactionDataPhone
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdPhone = transactionPhone.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionPhone);

        Transaction transactionOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transactionOuter.getData().getTransactionData();
        transactionDataOuter
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(12));
        transactionIdOuter = transactionOuter.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionOuter);

        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdServ1, transactionTypeServicePayment, BigDecimal.valueOf(12));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version2.toString(), transactionIdServ2, transactionTypeServicePayment, BigDecimal.valueOf(12));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdBudget, transactionTypeBudgetTransfer, BigDecimal.valueOf(12));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdPhone, transactionTypePhoneNumberTransfer, BigDecimal.valueOf(12));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdOuter, transactionTypeOuterTransfer, BigDecimal.valueOf(12));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdCard, transactionTypeCardTransfer, BigDecimal.valueOf(12));
    }

    @Test(
            description = "Провести транзакцию № 11, 11.1, 12, 13, 14, 15" +
                    "-- \"Оплата услуг\" (Version = 1, transactionID = 11), \"Оплата услуг\" (Version = 2, transactionID = 3), сумма 20" +
                    "-- \"Перевод на карту\" (Version = 1, transactionID = 12), сумма 20" +
                    "-- \"Перевод на счет\" (Version = 1, transactionID = 13), сумма 20" +
                    "-- \"Перевод в бюджет\" (Version = 1, transactionID = 14), сумма 20" +
                    "-- \"Перевод по номеру телефона\" (Version = 1, transactionID = 15), сумма 20" +
                    "Отклонить транзакцию 13" +
                    "Подтвердить транзакцию 14 (через алерт), но пометить транзакцию как мошенничество (action по транзакции)" +
                    "Подтвердить транзакции № 11, 11.1, 12, 15",
            dependsOnMethods = "secondTransactions"
    )

    public void transactionsAlertMaxAmount() {
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdServ1 = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdServ1);
        assertTableField("Версия документа:", version1.toString());

        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .withVersion(version2)
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdServ2 = transactionDataTwo.getTransactionId();
        sendAndAssert(transactionTwo);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdServ2);
        assertTableField("Версия документа:", version2.toString());

        Transaction transactionCard = getCardTransfer();
        TransactionDataType transactionDataCard = transactionCard.getData().getTransactionData();
        transactionDataCard
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdCard = transactionDataCard.getTransactionId();
        sendAndAssert(transactionCard);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Мошенничество")
                .sleep(1);
        assertTableField("Resolution:", "Мошенничество");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdCard);
        assertTableField("Версия документа:", version1.toString());

        Transaction transactionBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transactionBudget.getData().getTransactionData();
        transactionDataBudget
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdBudget = transactionBudget.getData().getTransactionData().getTransactionId();
        sendAndAssert(transactionBudget);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdBudget);
        assertTableField("Версия документа:", version1.toString());

        Transaction transactionPhone = getPhoneNumberTransfer();
        TransactionDataType transactionDataPhone = transactionPhone.getData().getTransactionData();
        transactionDataPhone
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdPhone = transactionDataPhone.getTransactionId();
        sendAndAssert(transactionPhone);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdPhone);
        assertTableField("Версия документа:", version1.toString());

        Transaction transactionOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transactionOuter.getData().getTransactionData();
        transactionDataOuter
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20));
        transactionIdOuter = transactionDataOuter.getTransactionId();
        sendAndAssert(transactionOuter);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Отклонить")
                .sleep(1);
        assertTableField("Resolution:", "Отклонено");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdOuter);
        assertTableField("Версия документа:", version1.toString());
    }

    @Test(
            description = "Запустить джоб PaymentMaxAmountJob и проверить \"Максимальная сумма транзакции\" ",
            dependsOnMethods = "transactionsAlertMaxAmount"
    )
    public void runJobStep2() {

        getIC().locateJobs()
                .selectJob("PaymentMaxAmountJob")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();

        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdServ1, transactionTypeServicePayment, BigDecimal.valueOf(20));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version2.toString(), transactionIdServ2, transactionTypeServicePayment, BigDecimal.valueOf(20));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdBudget, transactionTypeBudgetTransfer, BigDecimal.valueOf(20));
        assertPaymentMaxAmountVersionDoc(clientIds.get(0), version1.toString(), transactionIdPhone, transactionTypePhoneNumberTransfer, BigDecimal.valueOf(20));
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeOuterTransfer, BigDecimal.valueOf(12));
        assertPaymentMaxAmount(clientIds.get(0), transactionTypeCardTransfer, BigDecimal.valueOf(12));
        getIC().home();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        return transaction;
    }

    private Transaction getCardTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        return transaction;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        return transaction;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        return transaction;
    }

    private Transaction getPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        return transaction;
    }
}
