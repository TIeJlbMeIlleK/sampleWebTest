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

public class IR_03_RepeatApprovedTransactionBuyingInsurance extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Игорь", "Зерко", "Степанович"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Покупка страховки держателей карт")
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
                    "Покупка страховки держателей карт, проверить на отклонение суммы," +
                    "на совпадение остатка по счету, на длину серии, insuranceCompany, product",
            dependsOnMethods = "addClients"
    )

    public void transBuyingInsurance() {
        time.add(Calendar.HOUR, -20);
        Transaction transBuyingInsurance = getBuyingInsurance();
        sendAndAssert(transBuyingInsurance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Покупка страховки держателей карт», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceOutside = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceOutside = transBuyingInsuranceOutside.getData().getTransactionData();
        transactionDataBuyingInsuranceOutside
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(800.00));
        sendAndAssert(transBuyingInsuranceOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceAccountBalance = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceAccountBalance = transBuyingInsuranceAccountBalance.getData().getTransactionData();
        transactionDataBuyingInsuranceAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transBuyingInsuranceAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceInsuranceCompany = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceInsuranceCompany = transBuyingInsuranceInsuranceCompany.getData().getTransactionData();
        transactionDataBuyingInsuranceInsuranceCompany
                .getByuingInsurance()
                .withInsuranceCompany("Страховка");
        sendAndAssert(transBuyingInsuranceInsuranceCompany);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceProduct = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceProduct = transBuyingInsuranceProduct.getData().getTransactionData();
        transactionDataBuyingInsuranceProduct
                .getByuingInsurance()
                .withProduct("Любые случаи");
        sendAndAssert(transBuyingInsuranceProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceDeviation = getBuyingInsurance();
        TransactionDataType transactionDataBuyingInsuranceDeviation = transBuyingInsuranceDeviation.getData().getTransactionData();
        transactionDataBuyingInsuranceDeviation
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(372.25));
        sendAndAssert(transBuyingInsuranceDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Покупка страховки держателей карт» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transBuyingInsuranceLength = getBuyingInsurance();
        sendAndAssert(transBuyingInsuranceLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Покупка страховки держателей карт» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transBuyingInsurancePeriod = getBuyingInsurance();
        sendAndAssert(transBuyingInsurancePeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Покупка страховки держателей карт», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getBuyingInsurance() {
        Transaction transaction = getTransaction("testCases/Templates/BUYING_INSURANCE_Android.xml");
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
                .getByuingInsurance()
                .withInsuranceAmount(BigDecimal.valueOf(500.00))
                .withProduct("4 - Несч. случаи и болезни КК с доп. покрытием")
                .withInsuranceCompany("АО СК РСХБ-Страхование");
        return transaction;
    }
}
