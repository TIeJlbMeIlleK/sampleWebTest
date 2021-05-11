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

public class IR_03_RepeatApprovedTransactionOpenAccount extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    private final GregorianCalendar time = new GregorianCalendar();
    private final String productName = "Накопительный счёт";
    private final String sourceProduct = "40802020202073736464";
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Игорь", "Зерновой", "Петрович"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Открытие счёта (в том числе накопительного)")
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
                    "Открытие счёта (в том числе накопительного), проверить на отклонение суммы," +
                    "на совпадение остатка по счету, на длину серии, productName, productName",
            dependsOnMethods = "addClients"
    )

    public void transOpenAccount() {
        time.add(Calendar.HOUR, -20);
        Transaction transOpenAccount = getOpenAccount();
        TransactionDataType transactionDataOpenAccount = transOpenAccount.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOpenAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие счёта (в том числе накопительного)», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountOutside = getOpenAccount();
        TransactionDataType transactionDataOpenAccountOutside = transOpenAccountOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOpenAccountOutside
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOpenAccountOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountAccountBalance = getOpenAccount();
        TransactionDataType transactionDataOpenAccountAccountBalance = transOpenAccountAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOpenAccountAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transOpenAccountAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountSourceProduct = getOpenAccount();
        TransactionDataType transactionDataOpenAccountSourceProduct = transOpenAccountSourceProduct.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOpenAccountSourceProduct
                .getOpenAccount()
                .withSourceProduct("40801010101010101010");
        sendAndAssert(transOpenAccountSourceProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountProductName = getOpenAccount();
        TransactionDataType transactionDataOpenAccountProductName = transOpenAccountProductName.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOpenAccountProductName
                .getOpenAccount()
                .withProductName("Сберегательный счёт");
        sendAndAssert(transOpenAccountProductName);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountDeviation = getOpenAccount();
        TransactionDataType transactionDataOpenAccountDeviation = transOpenAccountDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOpenAccountDeviation
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOpenAccountDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Открытие счёта (в том числе накопительного)» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOpenAccountLength = getOpenAccount();
        TransactionDataType transactionDataOpenAccountLength = transOpenAccountLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOpenAccountLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Открытие счёта (в том числе накопительного)» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transOpenAccountPeriod = getOpenAccount();
        TransactionDataType transactionDataOpenAccountPeriod = transOpenAccountPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOpenAccountPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Открытие счёта (в том числе накопительного)», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getOpenAccount() {
        Transaction transaction = getTransaction("testCases/Templates/OPEN_ACCOUNT_Android.xml");
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
                .getOpenAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withProductName(productName)
                .withSourceProduct(sourceProduct);
        return transaction;
    }
}
