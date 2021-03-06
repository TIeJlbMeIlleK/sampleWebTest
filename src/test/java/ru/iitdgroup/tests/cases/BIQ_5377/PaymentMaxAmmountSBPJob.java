package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PaymentMaxAmmountSBPJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time2;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private Client client = null;
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private static final String RULE_NAME = "R01_GR_51_AnomalTransfer_TSP";
    private static final String REFERENCE_ITEM = "(Rule_tables)Максимальная сумма транзакции СБП по типам ТСП";
    private static final String TYPE_TSP = new RandomString(8).nextString();

    @Test(
            description = "Включить правило G51_AnomalTransfer_TSP" +
                    "-- период времени с максимальной суммы транзакции СБП = 1" +
                    "-- Отклонение 0.2"
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

        getIC().locateTable(REFERENCE_ITEM).deleteAll();
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
                this.client = client;
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Сумма 10, Тип ТСП = ТСП№1",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
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
                .match("Дата транзакции с максимальной суммой", format.format(time.getTime()))
                .failIfNoRows(); //проверка справочника на наличие записи после отработки JOB
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 Платеж по QR-коду через СБП -- Сумма 11, Тип ТСП = ТСП№1",
            dependsOnMethods = "runJobStep1"
    )

    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();

        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(11));
        sendAndAssert(transaction);
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 Платеж по QR-коду через СБП -- Сумма 13, Тип ТСП = ТСП№1" +
                    "-- Отклонить транзакцию №3",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(13));
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts().openFirst()
                .action("Отклонить").sleep(3);
        assertTableField("Resolution:", "Отклонено");
        assertTableField("Идентификатор клиента:", clientIds.get(0));

    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 Платеж по QR-коду через СБП -- Сумма 14, Тип ТСП = ТСП№1" +
                    "-- Отклонить как Мошенничество №4",
            dependsOnMethods = "step3"
    )

    public void step4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(14));
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
        time.add(Calendar.MINUTE, 1);
        time2 = (GregorianCalendar) time.clone();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(15));
        sendAndAssert(transaction);

        getIC().locateAlerts().refreshTable();
        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить").sleep(3);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Отправить транзакцию №6 от Клиента №1 Платеж по QR-коду через СБП -- Сумма 16, Тип ТСП = ТСП№1" +
                    "-- Подтвердить транзакцию = №6",
            dependsOnMethods = "step5"
    )

    public void step6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(16));
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
                .waitSeconds(5)
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
                .match("Дата транзакции с максимальной суммой", format.format(time.getTime()))
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
                .waitSeconds(5)
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
                .match("Дата транзакции с максимальной суммой", format.format(time2.getTime()))
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        return transaction;
    }
}
