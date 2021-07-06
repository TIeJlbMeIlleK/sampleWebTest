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

public class IR_03_RepeatApprovedTransactionClosureDeposit extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final String sourceProduct = "40802020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationProduct = "40802020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Елизавета", "Тураева", "Игоревна"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Закрытие вклада")
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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Закрытие вклада, проверить на отклонение суммы," +
                    "на совпадение остатка по счету и на длину серии",
            dependsOnMethods = "addClients"
    )

    public void transClosureDeposit() {
        time.add(Calendar.HOUR, -20);
        Transaction transClosureDeposit = getClosureDeposit();
        sendAndAssert(transClosureDeposit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие вклада», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositOutside = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositOutside = transClosureDepositOutside.getData().getTransactionData();
        transactionDataClosureDepositOutside
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transClosureDepositOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositAccountBalance = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositAccountBalance = transClosureDepositAccountBalance.getData().getTransactionData();
        transactionDataClosureDepositAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transClosureDepositAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositSourceProduct = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositSourceProduct = transClosureDepositSourceProduct.getData().getTransactionData();
        transactionDataClosureDepositSourceProduct
                .getClosureDeposit()
                .withSourceProduct("40802020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12));
        sendAndAssert(transClosureDepositSourceProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositProductName = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositProductName = transClosureDepositProductName.getData().getTransactionData();
        transactionDataClosureDepositProductName
                .getClosureDeposit()
                .withProductName("Закрытие");
        sendAndAssert(transClosureDepositProductName);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositDestinationProduct = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositDestinationProduct = transClosureDepositDestinationProduct.getData().getTransactionData();
        transactionDataClosureDepositDestinationProduct
                .getClosureDeposit()
                .withDestinationProduct("40802055" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12));
        sendAndAssert(transClosureDepositDestinationProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositDeviation = getClosureDeposit();
        TransactionDataType transactionDataClosureDepositDeviation = transClosureDepositDeviation.getData().getTransactionData();
        transactionDataClosureDepositDeviation
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transClosureDepositDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Закрытие вклада» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureDepositLength = getClosureDeposit();
        sendAndAssert(transClosureDepositLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие вклада» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transClosureDepositPeriod = getClosureDeposit();
        sendAndAssert(transClosureDepositPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие вклада», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getClosureDeposit() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_DEPOSIT.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withProductName("Вклад до востребования")
                .withSourceProduct(sourceProduct)
                .withDestinationProduct(destinationProduct);
        return transaction;
    }
}
