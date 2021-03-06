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

public class IR_03_RepeatApprovedTransactionBetween extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String destinationProduct = "40817810241000013266";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Зинаида", "Глухова", "Георгиевна"}};


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
                    "Перевод между счетами, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету, длину серии и destinationProduct",
            dependsOnMethods = "addClients"
    )

    public void transBetween() {
        time.add(Calendar.HOUR, -10);
        Transaction transBetween = getTransferBetweenAccounts();
        sendAndAssert(transBetween);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод между счетами», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenOutside = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenOutside = transBetweenOutside.getData().getTransactionData();
        transactionDataBetweenOutside
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transBetweenOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenAccountBalance = getTransferBetweenAccounts();
        transBetweenAccountBalance.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transBetweenAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenDestination = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenDestination = transBetweenDestination.getData().getTransactionData();
        transactionDataBetweenDestination
                .getTransferBetweenAccounts()
                .withDestinationProduct("40817810241000017777");
        sendAndAssert(transBetweenDestination);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenDeviation = getTransferBetweenAccounts();
        TransactionDataType transactionDataBetweenDeviation = transBetweenDeviation.getData().getTransactionData();
        transactionDataBetweenDeviation
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transBetweenDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод между счетами» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBetweenLength = getTransferBetweenAccounts();
        sendAndAssert(transBetweenLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод между счетами» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transBetweenPeriod = getTransferBetweenAccounts();
        sendAndAssert(transBetweenPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод между счетами», условия правила не выполнены");
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
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataType
                .getTransferBetweenAccounts()
                .withDestinationProduct(destinationProduct)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        return transaction;
    }
}
