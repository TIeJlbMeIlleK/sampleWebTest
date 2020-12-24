package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.Assert;
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


public class PaymentMaxAmmountSBPJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 24, 14, 10, 0);
    private final GregorianCalendar time1 = new GregorianCalendar(2020, Calendar.DECEMBER, 24, 14, 20, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2020, Calendar.DECEMBER, 24, 14, 25, 0);
    private final GregorianCalendar time3 = new GregorianCalendar(2020, Calendar.DECEMBER, 22, 12, 00, 0);

    private final List<String> clientIds = new ArrayList<>();
    private Client client = null;

    private static final String RULE_NAME = "R01_GR_51_AnomalTransfer_TSP";
    private static final String REFERENCE_ITEM = "(Rule_tables)Максимальная сумма транзакции СБП по типам ТСП";
    private static final String TYPE_TSP = "Пупкин";
    private static final String DATE_TIME = "24.12.2020 14:10:00";
    private static final String DATE_TIME1 = "24.12.2020 14:20:00";
    private static final String DATE_TIME2 = "24.12.2020 14:25:00";
    private static final String DATE_TIME3 = "22.12.2020 12:00:00";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName("Арина")
                        .withLastName("Афанасьева")
                        .withMiddleName("Андреевна")
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                this.client = client;
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test(
            description = "Включить правило G51_AnomalTransfer_TSP" +
                    "-- период времени с максимальной суммы транзакции СБП = 1" +
                    "-- Отклонение 0.2",
            dependsOnMethods = "addClient"
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
                .sleep(15);
    }

    @Test(
            description = "Очистить справочник «Максимальная сумма транзакции СБП по типам ТСП»",
            dependsOnMethods = "enableRules"
    )

    public void addMaxAmount() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.selectLinesAndDelete();
        }
    }

    @Test(
            description = "Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 10, Тип ТСП = ТСП№1",
            dependsOnMethods = "addMaxAmount"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);
    }

    @Test(
            description = "Запустить джоб PaymentMaxAmmountSBPJob и проверить справочник " +
                    "«Максимальная сумма транзакции СБП по типам ТСП» появилась запись:" +
                    "-- Клиент №1" +
                    "-- Максимальная сумма : 10" +
                    "-- Тип ТСП: ТСП№1" +
                    "-- Время транзакции из DocumentSaveTimeStamp транзакции №1",
            dependsOnMethods = "step1"
    )

    public void runJobStep1() {

        getIC().locateJobs()
                .selectJob("PaymentMaxAmountSBPJob")
                .addParameter("numberOfDays ", "1")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();
        getIC().locateTable(REFERENCE_ITEM)
                .setTableFilter("Тип ТСП", "Equals", TYPE_TSP)
                .refreshTable()
                .findRowsBy()
                .match("client", name)
                .match("Тип ТСП", TYPE_TSP)
                .match("Максимальная сумма", "10,00")
                .match("Дата транзакции с максимальной суммой", DATE_TIME)
                .failIfNoRows(); //проверка справочника на наличие записи после отработки JOB
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 11, Тип ТСП = ТСП№1",
            dependsOnMethods = "runJobStep1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(11))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 13, Тип ТСП = ТСП№1\n" +
                    "-- Отклонить транзакцию №3",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(13))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Отклонить").sleep(3);
        assertTableField("Resolution:", "Отклонено");
        assertTableField("Идентификатор клиента:", clientIds.get(0));

    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 14, Тип ТСП = ТСП№1" +
                    "-- Отклонить как Мошенничество №4",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(14))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts()
                .openFirst()
                .action("Мошенничество").sleep(3);
        assertTableField("Resolution:", "Мошенничество");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Отправить транзакцию №5 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 15, Тип ТСП = ТСП№1" +
                    "-- Подтвердить транзакцию = №5",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(15))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Отправить транзакцию №6 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 16, Тип ТСП = ТСП№1\n" +
                    "-- Подтвердить транзакцию = №6",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(16))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Запустить джоб PaymentMaxAmmountSBPJob во второй раз и проверить,что " +
                    "После второго запуска Джоба в справочнике «Максимальная сумма транзакции СБП по типам ТСП» появилась запись:" +
                    "-- Клиент №1" +
                    "-- Максимальная сумма : 16" +
                    "-- Тип ТСП: ТСП№1" +
                    "-- Время транзакции из DocumentSaveTimeStamp транзакции №6" +
                    "Далее Перейти в Алерт по транзакции №6 и Перевести транзакцию в Мошенничество",
            dependsOnMethods = "step6"
    )
    public void runJobStep2() {

        getIC().locateJobs()
                .selectJob("PaymentMaxAmountSBPJob")
                .addParameter("numberOfDays ", "1")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();
        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();
        getIC().locateTable(REFERENCE_ITEM)
                .setTableFilter("Тип ТСП", "Equals", TYPE_TSP)
                .refreshTable()
                .findRowsBy()
                .match("client", name)
                .match("Тип ТСП", TYPE_TSP)
                .match("Максимальная сумма", "16,00")
                .match("Дата транзакции с максимальной суммой", DATE_TIME2)
                .failIfNoRows(); //проверка справочника на наличие записи после отработки JOB

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts()
                .openFirst()
                .action("Мошенничество").sleep(3);
        assertTableField("Resolution:", "Мошенничество");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Запустить джоб PaymentMaxAmmountSBPJob в третий раз и" +
                    "После третьего запуска Джоба в справочнике «Максимальная сумма транзакции СБП по типам ТСП» появилась запись:" +
                    "-- Клиент №1" +
                    "-- Максимальная сумма : 15" +
                    "-- Тип ТСП: ТСП№1" +
                    "-- Время транзакции из DocumentSaveTimeStamp транзакции №5",
            dependsOnMethods = "runJobStep2"
    )
    public void runJobStep3() {

        getIC().locateJobs()
                .selectJob("PaymentMaxAmountSBPJob")
                .addParameter("numberOfDays ", "1")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();
        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();
        getIC().locateTable(REFERENCE_ITEM)
                .setTableFilter("Тип ТСП", "Equals", TYPE_TSP)
                .refreshTable()
                .findRowsBy()
                .match("client", name)
                .match("Дата транзакции с максимальной суммой", DATE_TIME1)
                .match("Максимальная сумма", "15,00")
                .match("Тип ТСП", TYPE_TSP)
                .failIfNoRows(); //проверка справочника на наличие записи после отработки JOB
        getIC().close();
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
