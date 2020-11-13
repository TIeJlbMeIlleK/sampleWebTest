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

    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

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
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 6; i++) {
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило и выполняем преднастройки",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Длина серии:","3")
                .fillInputText("Период серии в минутах:","10")
                .fillCheckBox("РДАК выполнен:",false)
                .fillCheckBox("АДАК выполнен:",true)
                .fillCheckBox("Требовать совпадения остатка на счете:",false)
                .select("Тип транзакции:","CARD_TRANSFER")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);

        commandServiceMock.run();
    }

    @Test(
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "step0"
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueADAKDone")
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
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("РДАК выполнен:",true)
                .fillCheckBox("АДАК выполнен:",false)
                .select("Тип транзакции:","CARD_TRANSFER")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);
    }

    @Test(
            description = " Отправить Транзакцию №3 в обработку от Клиента №2 -- Получатель №2, сумма 500" +
                    "-- выполнить rdakStat и SetResolutionContinue для транзакции",
            dependsOnMethods = "step2"
    )

    public void step3() {
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueRDAKDone")
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
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("РДАК выполнен:",true)
                .fillCheckBox("АДАК выполнен:",true)
                .select("Тип транзакции:","CARD_TRANSFER")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);
    }

    @Test(
            description = "Отправить Транзакцию №5 в обработку, от Клиента №3 -- Получатель №3, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueRDAKDone")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueADAKDone")
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
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueRDAKDone")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueADAKDone")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst().getActions()
                .doAction("Fraud")
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueRDAKDone")
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
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("ContinueADAKDone")
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
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step12"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
        getIC().close();
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
