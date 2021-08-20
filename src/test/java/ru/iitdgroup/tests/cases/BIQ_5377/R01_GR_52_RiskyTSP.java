package ru.iitdgroup.tests.cases.BIQ_5377;

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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class R01_GR_52_RiskyTSP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private static final String RULE_NAME = "R01_GR_52_RiskyTSP";
    private static final String REFERENCE_ITEM = "(Rule_tables) Рисковые ТСП";
    private static final String TSP_TYPE = "Предприниматель";
    private static final String TSP_NAME = "Киса Витальевич Воробьянинов";

    @Test(
            description = "Включить правило GR_52"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillInputText("Рисковый Тип ТСП или Имя ТСП:", TSP_NAME)
                .select("Тип рисковых данных:", "TSP_NAME")
                .save();

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Рисковый Тип ТСП или Имя ТСП:", TSP_TYPE)
                .select("Тип рисковых данных:", "TSP_TYPE")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
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
            description = "Отправить транзакцию №1 от Клиента №1 Платеж по QR-коду через СБП -- Тип ТСП из справочника",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withTSPName(null)
                .withTSPType(TSP_TYPE);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Рисковый Тип ТСП");
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 Платеж по QR-коду через СБП -- Наименование ТСП из справочника",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withTSPName(TSP_NAME)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Рисковое Имя ТСП");
    }

    @Test(
            description = " Отправить транзакцию №3 от Клиента №1 \"Платеж по QR-коду через СБП\" -- В транзакции нет Наименования ТСП и Типа ТСП",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В транзакции отсутствуют Тип ТСП и Имя ТСП");
    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 Платеж по QR-коду через СБП -- Тип ТСП и Наименование ТСП не из справочника",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withTSPName("Наименование не из справочника")
                .withTSPType("Тип не из справочника");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_NAME)
                .withTSPType(TSP_TYPE);
        return transaction;
    }

}
