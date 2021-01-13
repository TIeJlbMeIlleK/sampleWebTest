package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class R01_GR_52_RiskyTSP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 23, 14, 10, 0);
    private final List<String> clientIds = new ArrayList<>();

    private static final String RULE_NAME = "R01_GR_52_RiskyTSP";
    private static final String REFERENCE_ITEM = "(Rule_tables) Рисковые ТСП";

    private static final String TSP_TYPE = "Предприниматель";
    private static final String TSP_NAME = "Киса Витальевич Воробьянинов";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");


                    client.getData().getClientData().getClient()
                            .withFirstName("Николай")
                            .withLastName("Романов")
                            .withMiddleName("Иванович")
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
            description = "Добавить в справочник \"Рисковые ТСП\" \n" +
                    "-- Тип ТСП" +
                    "-- Наименование ТСП",
            dependsOnMethods = "addClient"
    )

    public void addRecipients() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Рисковый Тип ТСП или Имя ТСП:", TSP_NAME)
                .select("Тип рисковых данных:", "TSP_NAME")
                .save()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Рисковый Тип ТСП или Имя ТСП:", TSP_TYPE)
                .select("Тип рисковых данных:", "TSP_TYPE")
                .save()
                .sleep(5);
    }

    @Test(
            description = "Включить правило GR_52",
            dependsOnMethods = "addRecipients"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);
    }

    @Test(
            description = "Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Тип ТСП из справочника",
            dependsOnMethods = "enableRules"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(null)
                .withTSPType(TSP_TYPE);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Рисковый Тип ТСП");
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Наименование ТСП из справочника",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В транзакции отсутствуют Тип ТСП и Имя ТСП");
    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Тип ТСП и Наименование ТСП не из справочника",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
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
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
