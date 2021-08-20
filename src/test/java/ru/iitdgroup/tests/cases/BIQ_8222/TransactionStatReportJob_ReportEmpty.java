package ru.iitdgroup.tests.cases.BIQ_8222;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.report.ReportRecord;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.assertEquals;

public class TransactionStatReportJob_ReportEmpty extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private String transId;
    private final List<String> clientIds = new ArrayList<>();
    private static final String RULE_NAME = "";
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private String MILLISECONDS;

//TODO тест должен быть запущен в текущие 2 минуты, в которые не были отпарвлены транзакции .

    @Test(
            description = "Выключить все правила"
    )
    public void enableRules() {

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(10);
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
            description = "1. Для транзакции №1 увеличить REQUEST_TIMESTAMP для  типа SAF_RESPONSE на 20",
            dependsOnMethods = "addClient"
    )

    public void step1() throws SQLException {
        time.add(Calendar.SECOND, -5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData();
        transId = transactionDataType.getTransactionId();
        sendAndAssert(transaction);
        System.out.println("ID транзакции: № " + transId);

        MILLISECONDS = "20";
        getChangeDateBase();
    }

    @Test(
            description = "1. Для транзакции №2 увеличить REQUEST_TIMESTAMP для  типа SAF_RESPONSE на 150",
            dependsOnMethods = "step1"
    )

    public void step2() throws SQLException {
        time.add(Calendar.SECOND, 1);
        Transaction transaction1 = getTransaction();
        TransactionDataType transactionData = transaction1.getData().getTransactionData();
        transId = transactionData.getTransactionId();
        sendAndAssert(transaction1);
        System.out.println("ID транзакции: № " + transId);

        MILLISECONDS = "150";
        getChangeDateBase();
    }

    @Test(
            description = "5. Запустить джоб TransactionStatReportJob для построения отчета Отчет по времени ответа в ДБО в формате Excel" +
                    "Параметры джоба:" +
                    "startSelectPeriod=20.04.2021T17:22:00(время отправки транзакции)" +
                    "endSelectPeriod=20.04.2021T17:23:00 (Текущее время)" +
                    "emailRecipients=54321@mail.ru; 12345@mail.ru" +
                    "first=0" +
                    "second=700" +
                    "third=1500" +
                    "fourth=8999" +
                    "subject=TransactionStatReport",
            dependsOnMethods = "step2"
    )

    public void runJobStep() {
        time.add(Calendar.MINUTE, -2);
        String startTime = format.format(time.getTime()).replaceAll(" ", "T");
        String finishTime = format.format(time2.getTime()).replaceAll(" ", "T");
        getIC().locateJobs()
                .selectJob("TransactionStatReportJob")
                .addParameter("emailRecipients", "tafanasieva@iitdgroup.ru")
                .addParameter("startSelectPeriod", startTime)
                .addParameter("endSelectPeriod", finishTime)
                .addParameter("first", "200")
                .addParameter("second", "700")
                .addParameter("third", "1500")
                .addParameter("fourth", "8999")
                .run()
                .waitStatus(JobRunEdit.JobStatus.SUCCESS);
        getIC().home();
    }

    @Test(
            description = "6. Проверить выполнение джоба и отправку отчета Отчет по времени ответа в ДБО на почту",
            dependsOnMethods = "runJobStep"
    )

    public void step9() {
        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();

        assertEquals(res.getFildsValuesLog(1)[1], "EMAIL");//сверяет 1ю строчку 1ю колонку(Смежная система)
        assertEquals(res.getFildsValuesLog(1)[2], "EMAIL_GATEWAY");//сверяет 1ю строчку 2ю колонку(Тип сервиса)
        assertEquals(res.getFildsValuesLog(1)[4], "Yes");//сверяет 1ю строчку 6ю колонку(Успех)(отсекает пустые колонки)
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    public void getChangeDateBase() throws SQLException {
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String dateNew2;
        String dateNew1;

        String[][] result = getDatabase()
                .select()
                .field("REQUEST_TIMESTAMP")
                .field("DATEADD(millisecond, " + MILLISECONDS + ", REQUEST_TIMESTAMP)")//добавляет миллисекунды к дате в поле REQUEST_TIMESTAMP
                .from("WSLOGGED_MESSAGE_RSHB")//таблица
                .with("MESSAGE_TYPE", "=", "'TRANSACTION'")//фильтр таблицы по полю MESSAGE_TYPE со значением 'TRANSACTION'
                .sort("id", false)
                .limit(1)
                .get();
        dateNew1 = result[0][0];
        dateNew2 = result[0][1];//берем последнее значение даты
        dateNew1 = dateNew1.replaceAll(" ", "T");//заменяем пробел на букву анг."Т"
        dateNew2 = dateNew2.replaceAll(" ", "T");//заменяем пробел на букву анг."Т"

        System.out.println("Первоначальная дата: " + dateNew1);
        System.out.println("Дата с добавленными миллисекундами: " + dateNew2);

        HashMap<String, Object> map = new HashMap<>();
        map.put("REQUEST_TIMESTAMP", dateNew2);//обновляем таблицу WSLOGGED_MESSAGE_RSHB в БД на нужную дату
        getDatabase().updateWhere("WSLOGGED_MESSAGE_RSHB", map, String.format("WHERE [CORRELATION_ID]='%s'", transId));
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100.00));
        return transaction;
    }
}
