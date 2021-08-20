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

public class CheckTimeOutAndSendReportJobNotSent extends RSHBCaseTest {
//TODO в течении 2 минут не должны отправлять транзакции не из теста

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private String transId;
    private final List<String> clientIds = new ArrayList<>();
    private static final String RULE_NAME = "";
    private final String[][] names = {{"Олег", "Олегов", "Олегович"}};
    private String MILLISECONDS;
    private final String destinationCardNumber = "42756899" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

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
            description = "1. Транзакция №1 (Перевод на карту (Номер карты получателя -- 4578956321458796))," +
                    "сумма 1000. Увеличить по транзакции REQUEST_TIMESTAMP для  типа SAF_RESPONSE на 7000.",
            dependsOnMethods = "addClient"
    )

    public void step1() throws SQLException {
        time.add(Calendar.SECOND, -5);
        Transaction transaction = getTransactionCard();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData();
        transId = transactionDataType.getTransactionId();
        sendAndAssert(transaction);
        System.out.println("ID транзакции: № " + transId);

        MILLISECONDS = "7000";
        getChangeDateBase();
    }

    @Test(
            description = "1. Транзакция №2 (Перевод между счетами), сумма 1000." +
                    "Увеличить по транзакции REQUEST_TIMESTAMP для  типа SAF_RESPONSE на 8000.",
            dependsOnMethods = "step1"
    )

    public void step2() throws SQLException {
        time.add(Calendar.SECOND, 1);
        Transaction transaction1 = getTransactionOuter();
        TransactionDataType transactionData = transaction1.getData().getTransactionData();
        transId = transactionData.getTransactionId();
        sendAndAssert(transaction1);
        System.out.println("ID транзакции: № " + transId);

        MILLISECONDS = "8000";
        getChangeDateBase();
    }

    @Test(
            description = "2. Для транзакции №3 увеличить REQUEST_TIMESTAMP для  типа SAF_RESPONSE на 8900",
            dependsOnMethods = "step2"
    )

    public void step3() throws SQLException {
        time.add(Calendar.SECOND, 1);
        Transaction transaction2 = getTransactionServis();
        TransactionDataType transactionData1 = transaction2.getData().getTransactionData();
        transId = transactionData1.getTransactionId();
        sendAndAssert(transaction2);
        System.out.println("ID транзакции: № " + transId);

        MILLISECONDS = "8900";
        getChangeDateBase();
    }

    @Test(
            description = "5. Запустить джоб CheckTimeOutAndSendReportJob для создания" +
                    "и отправки на почту отчета: Отчет по времени ответа в ДБО с превышением таймаута в формате Excel" +
                    "Параметры джоба:" +
                    "startSelectPeriod=21.04.2021T13:30:00(дата отправки транзакций)" +
                    "endSelectPeriod=21.04.2021T13:34:00 (текущее)" +
                    "emailRecipients=54321@mail.ru; 12345@mail.ru" +
                    "criticalTimeResponse=8999" +
                    "subject=CheckTimeOutAndSendReport (если не писать данный параметр," +
                    " то по умолчанию будет написан Отчет по времени ответа в ДБО с" +
                    " превышением таймаута с 10.04.2021T00:00:00 по 21.04.2021T23:00:00)",
            dependsOnMethods = "step3"
    )

    public void runJobStep() {
        time.add(Calendar.MINUTE, -2);
        String startTime = format.format(time.getTime()).replaceAll(" ", "T");
        String finishTime = format.format(time2.getTime()).replaceAll(" ", "T");
        getIC().locateJobs()
                .selectJob("CheckTimeOutAndSendReportJob")
                .addParameter("emailRecipients", "tafanasieva@iitdgroup.ru")
                .addParameter("startSelectPeriod", startTime)
                .addParameter("endSelectPeriod", finishTime)
                .addParameter("criticalTimeResponse", "8999")
                .run()
                .waitStatus(JobRunEdit.JobStatus.SUCCESS);
        getIC().home();
    }

    @Test(
            description = "6. Проверить выполнение джоба и отправку отчета Отчет по времени ответа в ДБО на почту",
            dependsOnMethods = "runJobStep"
    )

    public void step4() {
        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();

        assertEquals(res.getFildsValuesLog(1)[1], "SAF");//сверяет 1ю строчку 1ю колонку(Смежная система)
        assertEquals(res.getFildsValuesLog(1)[2], "SAF_RESPONSE");//сверяет 1ю строчку 2ю колонку(Тип сервиса)
        assertEquals(res.getFildsValuesLog(1)[6], "Yes");//сверяет 1ю строчку 6ю колонку(Успех)(отсекает пустые колонки)
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
        dateNew1 = result[0][0];//первоначальное время без прибавления миллисекунд
        dateNew2 = result[0][1];//время с добавленными миллисекундами
        dateNew1 = dateNew1.replaceAll(" ", "T");//заменяем пробел на букву анг."Т"
        dateNew2 = dateNew2.replaceAll(" ", "T");//заменяем пробел на букву анг."Т"

        System.out.println("Первоначальная дата: " + dateNew1);
        System.out.println("Дата с добавленными миллисекундами: " + dateNew2);

        HashMap<String, Object> map = new HashMap<>();
        map.put("REQUEST_TIMESTAMP", dateNew2);//обновляем таблицу WSLOGGED_MESSAGE_RSHB в БД на нужную дату
        getDatabase().updateWhere("WSLOGGED_MESSAGE_RSHB", map, String.format("WHERE [CORRELATION_ID]='%s'", transId));
    }

    private Transaction getTransactionCard() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
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
                .getCardTransfer()
                .withDestinationCardNumber(destinationCardNumber)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        return transaction;
    }

    private Transaction getTransactionOuter() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
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
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        return transaction;
    }

    private Transaction getTransactionServis() {
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        return transaction;
    }
}
