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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TransferFromQuarantineToTypicalLocationJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"},
            {"Илья", "Зубов", "Кириллович"}, {"Олег", "Михайлов", "Иванович"}};
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM_QUARANTINE_LOCATION = "(Rule_tables) Карантин месторасположения";
    private static final String REFERENCE_ITEM_TUPICAL_LOCATION = "(Rule_tables) Типичное расположение";
    private static String clientFk1 = null;
    private static String clientFk2 = null;
    private static String clientFk3 = null;
    private static final Long version1 = 1L;
    private static final String ipAdress = "10.28.45.183";

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
                .findRowsBy()
                .match("код значения", "CLAIM_PERIOD")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "TRESHOLD_VISITS_TRANSFER_TO_TYPICAL_LOCATION")
                .click()
                .edit()
                .fillInputText("Значение:", "3")
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
            for (int i = 0; i < 3; i++) {
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
            description = "1. Провести ТРИ транзакции для Клиента № 1 с IP-адреса № 1",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
        try {
            String[][] hash = getDatabase()//сохраняем clientFk(id) из БД в переменную для дальнейшего использования
                    .select()
                    .field("id")
                    .from("Client")
                    .sort("id", false)
                    .limit(3)
                    .get();
            clientFk3 = hash[0][0];
            System.out.println(clientFk1);
            clientFk2 = hash[1][0];
            System.out.println(clientFk2);
            clientFk1 = hash[2][0];
            System.out.println(clientFk3);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        sendAndAssert(transaction);

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        sendAndAssert(transactionTwo);

        time.add(Calendar.SECOND, 20);
        Transaction transactionThree = getTransactionServicePayment();
        TransactionDataType transactionDataThree = transactionThree.getData().getTransactionData();
        sendAndAssert(transactionThree);
    }

    @Test(
            description = "1. Провести ДВЕ транзакции для Клиента № 2 с IP-адреса № 1",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {

        time.add(Calendar.MINUTE, -15);
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transaction);

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transactionTwo);
    }

    @Test(
            description = "1. Провести ТРИ транзакции для Клиента № 3 с IP-адреса № 1",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {

        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds().withDboId(clientIds.get(2));
        sendAndAssert(transaction);

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionServicePayment();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .getClientIds().withDboId(clientIds.get(2));
        sendAndAssert(transactionTwo);

        time.add(Calendar.SECOND, 20);
        Transaction transactionThree = getTransactionServicePayment();
        TransactionDataType transactionDataThree = transactionThree.getData().getTransactionData();
        transactionDataThree
                .getClientIds().withDboId(clientIds.get(2));
        sendAndAssert(transactionThree);

        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "3")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(1))
                .match("Кол-во посещений", "2")
                .failIfNoRows();//проверка справочника на наличие записи
        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(2))
                .match("Кол-во посещений", "3")
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Test(
            description = "Изменить В \"Карантин месторасположения \"Дата первого посещения\"" +
                    "для появившихся записей: для клиентов№1 и №2 на дату более 1 дня назад" +
                    "для клиента№3 на дату менее одного дня" +
                    "Запустить джоб TransferFromQuarantineToTypicalLocationJob и проверить карантин расположения" +
                    "и Типичное расположение",
            dependsOnMethods = "transaction3"
    )
    public void runJob() {
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("FIRST_DATE_VISIT", Instant.now().minus(40, ChronoUnit.HOURS).toString());
        getDatabase().updateWhere("QUARANTINE_LOCATION", map1, "WHERE [CLIENT_FK] =" + clientFk1);

        HashMap<String, Object> map2 = new HashMap<>();
        map2.put("FIRST_DATE_VISIT", Instant.now().minus(32, ChronoUnit.HOURS).toString());
        getDatabase().updateWhere("QUARANTINE_LOCATION", map2, "WHERE [CLIENT_FK] =" + clientFk2);

        HashMap<String, Object> map3 = new HashMap<>();
        map3.put("FIRST_DATE_VISIT", Instant.now().minus(12, ChronoUnit.HOURS).toString());
        getDatabase().updateWhere("QUARANTINE_LOCATION", map3, "WHERE [CLIENT_FK] =" + clientFk3);

        getIC().locateJobs()
                .selectJob("TransferFromQuarantineToTypicalLocationJob")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка наличия записи в типичном
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .match("Кол-во посещений", "3")
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(1))
                .match("Кол-во посещений", "2")
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(2))
                .match("Кол-во посещений", "3")
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM_QUARANTINE_LOCATION)//проверка удаления записи из карантина после отработки JOB
                .refreshTable()
                .findRowsBy()
                .match("cifID", clientIds.get(0))
                .failIfRowsExists();//проверка справочника на отсутствие записи
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getClientDevice().getAndroid().withIpAddress(ipAdress);
        return transaction;
    }
}
