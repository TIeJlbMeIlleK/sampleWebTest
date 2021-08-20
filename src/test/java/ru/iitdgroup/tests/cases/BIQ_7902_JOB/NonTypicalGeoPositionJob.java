package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class NonTypicalGeoPositionJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private GregorianCalendar cloneTime;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}, {"Илья", "Зубов", "Кириллович"}};
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры сбора нетипичной геопозиции";
    private static final String REFERENCE_ITEM_QUARANTINE_LOCATION = "(Rule_tables) Карантин месторасположения";
    private static final String REFERENCE_ITEM_TUPICAL_LOCATION = "(Rule_tables) Типичное расположение";
    private static final Long version1 = 1L;
    private static final String ipAdress_tupical = "193.106.170.62";
    private static final String ipAdress2 = "213.87.156.92";

    @Test(
            description = "Включить правило; остальные правила деактивированы"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Точность сравнения координат:", "0,5")
                .save()
                .sleep(15);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillCheckBox("Требуется сбор нетипичной геопозиции:", true)
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод в сторону государства")
                .save();

        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION).deleteAll();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION).deleteAll();
    }

    @Test(
            description = "Создаем клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 2; i++) {
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
        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Страна:", "Россия")
                .fillInputText("Долгота:", "91,42")
                .fillInputText("Широта:", "53,71")
                .fillInputText("Кол-во посещений:", "1")
                .fillInputText("Дата последней транзакции:", format.format(time.getTime()))
                .save();
    }

    @Test(
            description = "1. Провести транзакцию № 1 \"Перевод в сторону государства\" с месторасположения № 1 для Клиента № 1" +
                    "2. Провести транзакцию № 2 \"Перевод на карту\" с месторасположения № 2 для Клиента № 1" +
                    "3. Провести транзакцию № 3 \"Перевод в сторону государства\" с месторасположения № 2 для Клиента № 1 (с новой датой)" +
                    "4. Провести транзакцию № 4 \"Перевод в сторону государства\" с месторасположения № 1 для Клиента № 2" +
                    "5. Проверить \"Типичное месторасположение\" и \"Карантин месторасположения\"",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {

        Transaction transaction = getBudgetTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        sendAndAssert(transaction);

        Transaction transactionCard = getTransactionCARD();
        TransactionDataType transactionDataCard = transactionCard.getData().getTransactionData();
        sendAndAssert(transactionCard);

        time2.add(Calendar.SECOND, 5);
        cloneTime = (GregorianCalendar) time2.clone();
        Transaction transactionBudget = getBudgetTransfer();
        TransactionDataType transactionDataBudget = transactionBudget.getData().getTransactionData();
        transactionDataBudget
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(cloneTime))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(cloneTime));
        transactionDataBudget
                .getClientDevice()
                .getAndroid()
                .withIpAddress(ipAdress2);
        sendAndAssert(transactionBudget);

        Transaction transactionBudg = getBudgetTransfer();
        TransactionDataType transactionDataBudg = transactionBudg.getData().getTransactionData();
        transactionDataBudg
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transactionBudg);

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "2")
                .match("Долгота", "91.42")
                .match("Широта", "53.71")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "2")
                .match("Долгота", "37.62")
                .match("Широта", "55.75")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(1))
                .match("Кол-во посещений", "1")
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Test(
            description = "Запустить джоб NonTypicalGeoPositionJob и проверить карантин расположения" +
                    "и Типичное расположение",
            dependsOnMethods = "transaction1"
    )
    public void runJob() {

        getIC().locateJobs()
                .selectJob("NonTypicalGeoPositionJob")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "3")
                .match("Долгота", "91.42")
                .match("Широта", "53.71")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "3")
                .match("Долгота", "37.62")
                .match("Широта", "55.75")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(1))
                .match("Кол-во посещений", "2")
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Test(
            description = "Запустить джоб NonTypicalGeoPositionJob во второй раз и проверить карантин расположения" +
                    "и Типичное расположение. Последний Джоб не берет в расчет транзакции, выполненные до сработки предыдущего Джоба," +
                    "записи в Типичном и Карантине не должны измениться. В расчет берутся транзакции поступившие после" +
                    "сработки предыдущего Джоба и перед запуском последнего Джоба",
            dependsOnMethods = "transaction1"
    )
    public void runJobTwo() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getBudgetTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        sendAndAssert(transaction);

        time.add(Calendar.MINUTE, -20);
        Transaction transactionBudg = getBudgetTransfer();
        TransactionDataType transactionDataBudg = transactionBudg.getData().getTransactionData();
        transactionDataBudg
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transactionBudg);

        getIC().locateJobs()
                .selectJob("NonTypicalGeoPositionJob")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "4")
                .match("Долгота", "91.42")
                .match("Широта", "53.71")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "3")
                .match("Долгота", "37.62")
                .match("Широта", "55.75")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(1))
                .match("Кол-во посещений", "3")
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getBudgetTransfer()
                .withUIN("0")
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        transaction.getData().getTransactionData()
                .getClientDevice().getAndroid().withIpAddress(ipAdress_tupical);
        return transaction;
    }

    private Transaction getTransactionCARD() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice().getAndroid().withIpAddress(ipAdress2);
        return transaction;
    }
}
