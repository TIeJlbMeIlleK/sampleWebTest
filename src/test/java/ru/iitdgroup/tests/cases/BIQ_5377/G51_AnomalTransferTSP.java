package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import java.util.*;
import java.text.*;

import static org.testng.Assert.fail;


public class G51_AnomalTransferTSP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 23, 14, 10, 0);
    private final List<String> clientIds = new ArrayList<>();

    private static final String RULE_NAME = "R01_GR_51_AnomalTransfer_TSP";
    private static final String REFERENCE_ITEM = "(Rule_tables)Максимальная сумма транзакции СБП по типам ТСП";
    private static final String TYPE_TSP = "Рандом";
    private static final String DATE_TRANSACTION = "21.12.2020 15:10";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                if (i == 0) {
                    client.getData().getClientData().getClient()
                            .withFirstName("Ульяна")
                            .withLastName("Пономарева")
                            .withMiddleName("Игоревна")
                            .getClientIds()
                            .withDboId(dboId);
                } else if (i == 1){
                    client.getData().getClientData().getClient()
                            .withFirstName("Ирина")
                            .withLastName("Кукушкина")
                            .withMiddleName("Вячеславовна")
                            .getClientIds()
                            .withDboId(dboId);
                } else {
                    client.getData().getClientData().getClient()
                            .withFirstName("Маргарита")
                            .withLastName("Зыкина")
                            .withMiddleName("Михайловна")
                            .getClientIds()
                            .withDboId(dboId);
                }
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Добавить в справочник «Максимальная сумма транзакции СБП по типам ТСП» клиента №2\n" +
                    "-- Тип ТСП = ТСП№3\n" +
                    "-- Максимальная сумма = 20\n" +
                    "-- Дата транзакции с максимальной суммой = Текущее время - 2 дней",
            dependsOnMethods = "addClient"
    )

    public void addMaxAmount() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

//        Date dateNow = new Date();
//        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yyyy hh:mm");

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Максимальная сумма:", "20")
                .fillInputText("Тип ТСП:", TYPE_TSP)
                .fillInputText("Дата транзакции с максимальной суммой:", DATE_TRANSACTION)
                .fillUser("client:", clientIds.get(1))
                .save()
                .sleep(10);
    }

    @Test(
            description = "Включить правило \n" +
                    "-- Количество дней, за которые осуществляется набор статистических данных = 1\n" +
                    "-- Значение в сотых долях- допустимый порог 0.2",
            dependsOnMethods = "addMaxAmount"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Значение в сотых долях- допустимый порог:", "0,2")
                .fillInputText("Количество дней, за которые осуществляется набор статистических данных:", "1")
                .save()
                .sleep(20);
    }


    @Test(
            description = "Отправить транзакцию №1 \"Платеж по QR-коду через СБП\" -- Сумма 10, Тип ТСП = ТСП№1",
            dependsOnMethods = "enableRules"
    )

    public void step0() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName("Рандом № 1")
                .withTSPType("Рандом № 1");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Это первая транзакция для клиента или в справочнике «Максимальная сумма транзакции СБП по типам ТСП» отсутствуют максимальные суммы");
    }

    @Test(
            description = "Запустить джоб обновления MaxAmmount для платежей СБП",
            dependsOnMethods = "step0"
    )

    public void runJobStep1() {

            getIC().locateJobs()
                    .selectJob("PaymentMaxAmountSBPJob")
                    .addParameter("numberOfDays ", "1")
                    .waitSeconds(10)
                    .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                    .run().sleep(5);

            getIC().close();

    }

    @Test(
            description = "Отправить транзакцию №2 \"Платеж по QR-коду через СБП\" -- Сумма 11, Тип ТСП = ТСП№1",
            dependsOnMethods = "runJobStep1"
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(11))
                .withTSPName("Рандом № 1")
                .withTSPType("Рандом № 1");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Отправить транзакцию №3 \"Платеж по QR-коду через СБП\" -- Сумма 20, Тип ТСП = ТСП№1",
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(20))
                .withTSPName("Рандом № 1")
                .withTSPType("Рандом № 1");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Превышение порога отклонения от максимальной суммы транзакции Клиента для конкретного ТСП");
    }

    @Test(
            description = "Отправить транзакцию №4 \"Платеж по QR-коду через СБП\" -- Сумма 11, Тип ТСП = ТСП№2",
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(11))
                .withTSPName("Рандом № 2")
                .withTSPType("Рандом № 2");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = " Отправить транзакцию №5 \"Платеж по QR-коду через СБП\" -- Сумма 20, Тип ТСП = ТСП№2",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20))
                .withTSPName("Рандом № 2")
                .withTSPType("Рандом № 2");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Превышение порога отклонения от максимальной суммы транзакции Клиента независимо от ТСП");
    }

    @Test(
            description = "Отправить транзакцию №6 от Клиента №2 \"Платеж по QR-коду через СБП\" -- Сумма 23, Тип ТСП = ТСП№3",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(23))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Отправить транзакцию №7 от Клиента №2 \"Платеж по QR-коду через СБП\" -- Сумма 25, Тип ТСП = ТСП№3",
            dependsOnMethods = "step6"
    )

    public void step7() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(25))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Превышение порога отклонения от максимальной суммы транзакции Клиента независимо от ТСП");
    }

    @Test(
            description = "Отправить транзакцию №8 от Клиента №3 \"Платеж по QR-коду через СБП\" -- Сумма любая, без типа ТСП",
            dependsOnMethods = "step7"
    )

    public void step8() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(30))
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Это первая транзакция для клиента или в справочнике «Максимальная сумма транзакции СБП по типам ТСП» отсутствуют максимальные суммы");
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
