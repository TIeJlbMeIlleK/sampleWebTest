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

public class IR_03_RepeatApprovedTransactionClosureAccount extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final String productName = "Текущий счет";
    private final String sourceProduct = "40802020202087879898";
    private final String destinationProduct = "40802020202032323636";
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Сергей", "Зупаров", "Альбертович"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Закрытие счёта (в том числе накопительного)")
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
                    "Закрытие счёта (в том числе накопительного), проверить на отклонение суммы," +
                    "на совпадение остатка по счету, на длину серии, destinationProduct, sourceProduct, productName",
            dependsOnMethods = "addClients"
    )

    public void transClosureAccount() {
        time.add(Calendar.HOUR, -20);
        Transaction transClosureAccount = getClosureAccount();
        TransactionDataType transactionDataClosureAccount = transClosureAccount.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transClosureAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие счёта (в том числе накопительного)», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountOutside = getClosureAccount();
        TransactionDataType transactionDataClosureAccountOutside = transClosureAccountOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountOutside
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transClosureAccountOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountBalance = getClosureAccount();
        TransactionDataType transactionDataClosureAccountBalance = transClosureAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transClosureAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountSourceProduct = getClosureAccount();
        TransactionDataType transactionDataClosureAccountSourceProduct = transClosureAccountSourceProduct.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountSourceProduct
                .getClosureAccount()
                .withSourceProduct("40801010101010101010");
        sendAndAssert(transClosureAccountSourceProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountProductName = getClosureAccount();
        TransactionDataType transactionDataClosureAccountProductName = transClosureAccountProductName.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountProductName
                .getClosureAccount()
                .withProductName("Накопительный счёт");
        sendAndAssert(transClosureAccountProductName);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountDestinationProduct = getClosureAccount();
        TransactionDataType transactionDataClosureAccountDestinationProduct = transClosureAccountDestinationProduct.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountDestinationProduct
                .getClosureAccount()
                .withDestinationProduct("40801010101010555656");
        sendAndAssert(transClosureAccountDestinationProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountDeviation = getClosureAccount();
        TransactionDataType transactionDataClosureAccountDeviation = transClosureAccountDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataClosureAccountDeviation
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transClosureAccountDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Закрытие счёта (в том числе накопительного)» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transClosureAccountLength = getClosureAccount();
        TransactionDataType transactionDataClosureAccountLength = transClosureAccountLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transClosureAccountLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Закрытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transClosureAccountPeriod = getClosureAccount();
        TransactionDataType transactionDataClosureAccountPeriod = transClosureAccountPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transClosureAccountPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Закрытие счёта (в том числе накопительного)», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getClosureAccount() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_ACCOUNT.xml");
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
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withProductName(productName)
                .withSourceProduct(sourceProduct)
                .withDestinationProduct(destinationProduct);
        return transaction;
    }
}
