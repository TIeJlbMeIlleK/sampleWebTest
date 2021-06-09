package ru.iitdgroup.tests.cases.BIQ_6250;

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

public class IR_03_RepeatApprovedTransaction_PhoneNTRdakAdak extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String PAYEE_1 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 11);
    private static final String PAYEE_2 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 11);
    private static final String PAYEE_3 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 11);
    private final String[][] names = {{"Леонид", "Жуков", "Игоревич"}, {"Петр", "Серебряков", "Иванович"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

//  TODO  Перед выполнением ТК, требуется создать Actions:
//   6. Создан ручной Action WF в транзакции Continue, на изменение: Status = Complete и Resolution = Continue
//   ExternalApi -- Update Transaction In Cache.
//   7. Создан ручной Action WF в транзакции Fraud, на изменение: Status = Complete и Resolution = FRAUD
//   ExternalApi -- Update Transaction In Cache.
//   8. Создан ручной Action WF (ContinueRDAKDone) для Транзакции где в FieldMapping: Статус РДАК = Success
//   ExternalApi -- Update Transaction In Cache.
//   9. Создан ручной Action WF (ContinueADAKDone) для Транзакции где в FieldMapping: Статус АДАК = Success
//   ExternalApi -- Update Transaction In Cache.

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void ruleEdit() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", true)
                .fillCheckBox("РДАК выполнен:", true)
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .sleep(15);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод по номеру телефона")
                .select("Наименование канала:", "Мобильный банк")
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "ruleEdit"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 2; i++) {
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
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("АДАК выполнен")
                .approved();
    }

    @Test(
            description = "Отправить две Транзакции в обработку -- Получатель №1, сумма 372,25",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.SECOND, 20);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transactionTwo);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = " Отправить Транзакцию №3 в обработку от Клиента №1 -- Получатель №2, сумма 500" +
                    "-- выполнить rdakStat и SetResolutionContinue для транзакции",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("РДАК выполнен")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку от Клиента №2 -- Получатель №2, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №5 в обработку, от Клиента №2 -- Получатель №3, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_2);
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("РДАК выполнен")
                .approved();

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst()
                .getActions()
                .doAction("АДАК выполнен")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №6 в обработку от Клиента №2 -- Получатель №3, сумма 500",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №7 в обработку, от Клиента №4 -- Получатель №4, сумма 500",
            dependsOnMethods = "step6"
    )

    public void step7() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_3);
        String tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список транзакций")
                .setTableFilterForTransactions("ID транзакции", "Equals", tranID)
                .runReport()
                .openFirst().getActions()
                .doAction("Мошенничество")
                .approved();
    }

    @Test(
            description = "Отправить Транзакцию №8 в обработку от Клиента №4 -- Получатель №4, сумма 500",
            dependsOnMethods = "step7"
    )

    public void step8() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_3);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getPhoneNumberTransfer()
                .withPayeePhone(PAYEE_1);
        transaction.getData().getTransactionData()
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .withBankName("РОССЕЛЬХОЗБАНК");
        return transaction;
    }
}
