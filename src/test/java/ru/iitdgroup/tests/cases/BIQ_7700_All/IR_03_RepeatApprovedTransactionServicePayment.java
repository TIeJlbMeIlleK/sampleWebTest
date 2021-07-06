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

public class IR_03_RepeatApprovedTransactionServicePayment extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Елена", "Тырина", "Андреевна"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Оплата услуг")
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
                    "Оплата услуг, проверить на отклонение суммы," +
                    "на совпадение остатка по счету, на длину серии, account, providerName, serviceName",
            dependsOnMethods = "addClients"
    )

    public void transServis() {
        time.add(Calendar.MINUTE, -20);
        Transaction transService = getServicePayment();
        sendAndAssert(transService);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Оплата услуг», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceOutside = getServicePayment();
        TransactionDataType transactionDataServiceOutside = transServiceOutside.getData().getTransactionData();
        transactionDataServiceOutside
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transServiceOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceAccountBalance = getServicePayment();
        TransactionDataType transactionDataServiceAccountBalance = transServiceAccountBalance.getData().getTransactionData();
        transactionDataServiceAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transServiceAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceProviderName = getServicePayment();
        TransactionDataType transactionDataServiceProviderName = transServiceProviderName.getData().getTransactionData();
        transactionDataServiceProviderName
                .getServicePayment()
                .withProviderName("МТС");
        sendAndAssert(transServiceProviderName);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServiceName = getServicePayment();
        TransactionDataType transactionDataServiceName = transServiceName.getData().getTransactionData();
        transactionDataServiceName
                .getServicePayment()
                .withServiceName("МТС по телефону");
        sendAndAssert(transServiceName);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transServisDeviation = getServicePayment();
        TransactionDataType transactionDataServisDeviation = transServisDeviation.getData().getTransactionData();
        transactionDataServisDeviation
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transServisDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Оплата услуг» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transServisLength = getServicePayment();
        sendAndAssert(transServisLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Оплата услуг» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transServisPeriod = getServicePayment();
        sendAndAssert(transServisPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Оплата услуг», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withServiceKind("222")
                .withProviderName("Мегафон")
                .withServiceName("Мегафон по номеру телефона");
        return transaction;
    }
}
