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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JOB_PayeeToWhiteListRDAKTSP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar timeOf1stTransaction;
    private GregorianCalendar timeOf2ndTransaction;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final List<String> clientIds = new ArrayList<>();
    private Client client = null;
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Карантин получателей";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";
    private static final String TYPE_TSP1 = new RandomString(8).nextString();
    private static final String TYPE_TSP2 = new RandomString(8).nextString();
    private final String[][] names = {{"Петр", "Зыков", "Викторович"}};

    @Test(
            description = "Включить и настроить правило GR_20 и " +
                    "Очистить справочник «Карантин Получателей» и «Доверенные Получатели»" +
                    "Установить CLAIM_PERIOD = 1  (справочник Параметры обработки справочников и флагов)"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM1)
                .deleteAll();

        getIC().locateTable(REFERENCE_ITEM2)
                .deleteAll();

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy().match("код значения", "MANUAL_AUTHENTIFICATION_TIME_LIMIT").click()
                .edit().fillInputText("Значение:", "1").save().sleep(1);
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
            description = "Установить в БД в карточке клиента даты Смены учетных записей и Смены IMSI",
            dependsOnMethods = "addClient"
    )
    public void refactorWF() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("AUTH_CHANGE_TIMESTAMP", Instant.now().toString());
        map.put("AUTH_IMSI_DATE_CHANGE", Instant.now().toString());
        getDatabase().updateWhere("Client", map, "WHERE [DBO_ID] = " + clientIds.get(0));
    }

    @Test(
            description = " Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1 и" +
                    "Изменить в справочнике \"Карантин получателей\" \"Дата успешного РДАК\" на более 1 дня назад для транзакции №1",
            dependsOnMethods = "enableRules"
    )

    public void step1() {
        time.add(Calendar.MINUTE, 1);
        timeOf1stTransaction = (GregorianCalendar) time.clone();
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);

        HashMap<String, Object> map = new HashMap<>();//Изменяем в базе дату РДАК
        map.put("SUCCESSFUL_RDAK_DATE", Instant.now().minus(25, ChronoUnit.HOURS).toString());
        getDatabase().updateWhere("QUARANTINE_LIST", map, "WHERE [id] = (SELECT MAX([id]) FROM [QUARANTINE_LIST])");
    }

    @Test(
            description = " Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №2 и" +
                    "Изменить в справочнике \"Карантин получателей\" \"Дата успешного РДАК\" на менее 1 дня назад для транзакций №2",
            dependsOnMethods = "step1"
    )

    public void step2() {
        time.add(Calendar.MINUTE, 1);
        timeOf2ndTransaction = (GregorianCalendar) time.clone();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withTSPName(TYPE_TSP1)
                .withTSPType(TYPE_TSP1);
        sendAndAssert(transaction);

        HashMap<String, Object> map = new HashMap<>();//Изменяем в базе дату РДАК
        map.put("SUCCESSFUL_RDAK_DATE", Instant.now().minus(20, ChronoUnit.HOURS).toString());
        getDatabase().updateWhere("QUARANTINE_LIST", map, "WHERE [id] = (SELECT MAX([id]) FROM [QUARANTINE_LIST])");
    }

    @Test(
            description = "Запустить джоб PayeeToWhiteListRDAK",
            dependsOnMethods = "step2"
    )

    public void runJobStep() {
        getIC().locateJobs()
                .selectJob("PayeeToWhiteListRDAK")
                .run()
                .waitStatus(JobRunEdit.JobStatus.SUCCESS);
        getIC().home();
    }

    @Test(
            description = "Проверить Доверенные получатели и Карантин получателей",
            dependsOnMethods = "runJobStep"
    )

    public void checkingReferenceBooks() {
        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();

        getIC().locateTable(REFERENCE_ITEM2)//проверка доверенных
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP2)
                .match("Дата последней авторизованной транзакции", format.format(timeOf1stTransaction.getTime()))
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM1)//проверка карантина
                .refreshTable()
                .findRowsBy()
                .match("Дата последней авторизованной транзакции", format.format(timeOf2ndTransaction.getTime()))
                .match("Имя получателя", TYPE_TSP1)
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM1)//проверка карантина
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP2)
                .match("Дата последней авторизованной транзакции", format.format(timeOf1stTransaction.getTime()))
                .match("ФИО Клиента", name)
                .failIfRowsExists();

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
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        return transaction;
    }
}
