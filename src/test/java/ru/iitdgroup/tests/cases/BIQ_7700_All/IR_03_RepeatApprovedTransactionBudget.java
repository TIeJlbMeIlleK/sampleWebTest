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

public class IR_03_RepeatApprovedTransactionBudget extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Екатерина", "Мыркина", "Игоревна"}};

//    @Test(
//            description = "Включаем правило"
//    )
//
//    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .editRule(RULE_NAME)
//                .fillCheckBox("Active:", true)
//                .fillCheckBox("АДАК выполнен:", false)
//                .fillCheckBox("РДАК выполнен:", false)
//                .fillCheckBox("Требовать совпадения остатка на счете:", true)
//                .fillInputText("Длина серии:", "2")
//                .fillInputText("Период серии в минутах:", "10")
//                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
//                .save()
//                .detachWithoutRecording("Типы транзакций")
//                .attachIR03SelectAllType()
//                .sleep(20);

//        getIC().locateTable(REFERENCE_TABLE)
//                .deleteAll()
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод в сторону государства")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();
//   }

    @Test(
            description = "Создание клиентов"
            //dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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
                    "Перевод в сторону государства, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету, длину серии и uIN",
            dependsOnMethods = "addClients"
    )

    public void transBudget() {
        time.add(Calendar.HOUR, -10);
        Transaction transBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transBudget.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transBudget);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод в сторону государства», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetOutside = getBudgetTransfer();
        TransactionDataType transactionDataBudgetOutside = transBudgetOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataBudgetOutside
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transBudgetOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetAccountBalance = getBudgetTransfer();
        TransactionDataType transactionDataBudgetAccountBalance = transBudgetAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataBudgetAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transBudgetAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetUIN = getBudgetTransfer();
        TransactionDataType transactionDataBudgetUIN = transBudgetUIN.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataBudgetUIN
                .getBudgetTransfer()
                .withUIN("1");
        sendAndAssert(transBudgetUIN);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetDeviation = getBudgetTransfer();
        TransactionDataType transactionDataBudgetDeviation = transBudgetDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataBudgetDeviation
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transBudgetDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод в сторону государства» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBudgetLength = getBudgetTransfer();
        TransactionDataType transactionDataBudgetLength = transBudgetLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transBudgetLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод в сторону государства» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transBudgetPeriod = getBudgetTransfer();
        TransactionDataType transactionDataBudgetPeriod = transBudgetPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transBudgetPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод в сторону государства», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withUIN("0")
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        return transaction;
    }
}
