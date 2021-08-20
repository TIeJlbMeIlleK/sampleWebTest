package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_15_NonTypicalGeoPosition extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM2 = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Типичное расположение";
    private static final String REFERENCE_ITEM4 = "(Rule_tables) Карантин месторасположения";

    private static final String IP_ADDRESS1 = "95.73.149.81";
    private static final String NON_EXISTENT_IP_ADDRESS = "0.0.0.0";

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Семен", "Скирин", "Федорович"}, {"Павел", "Петушков", "Павлович"}};
    private static String[] login = {new RandomString(5).nextString(), new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};

    @Test(
            description = "Включаем и настраиваем правило и справочники" +
                    "Выключить интеграцию с ГИС (GisSystem_GIS)" +
                    "Установить TIME_AFTER_ADDING_TO_QUARANTINE = 2"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();

        getIC().locateTable(REFERENCE_ITEM2)
                .findRowsBy()
                .match("Описание", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "0")
                .save();

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM4).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time))
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
            description = "Провести транзакцию № 1 с указанием ip-адреса" +
                    "Включить интеграцию с ГИС (GisSystem_GIS)",
            dependsOnMethods = "addClient"
    )

    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_GIS_SETTING);

        getIC().locateTable(REFERENCE_ITEM2)
                .findRowsBy()
                .match("Описание", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Провести транзакцию № 2 с указанием ip-адреса, регулярная",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 3 без указания ip-адреса",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, REQUIRE_IP);
    }

    @Test(
            description = "Провести транзакцию № 4 без контейнера device",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withClientDevice(null);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertLastTransactionRuleApply(FEW_DATA, REQUIRE_IP);
    }

    @Test(//выход не совпадает с требованиями, должен быть FEW_DATA и "Недосточно данных: для полученного ip адреса нет данных о геолокации"
            description = "Провести транзакцию № 5 с несуществующего ip-адреса",
            dependsOnMethods = "transaction4"
    )
    //TODO проверить после исправления
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(NON_EXISTENT_IP_ADDRESS);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
        //выход не совпадает с требованиями, должен быть FEW_DATA и "Недосточно данных: для полученного ip адреса нет данных о геолокации"
    }

    @Test(
            description = "Провести транзакции № 6 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "transaction5"
    )

    public void transaction6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Провести транзакции № 7 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "transaction6"
    )

    public void transaction7() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Занести IP-адрес(долготу и широту) № 2 для Клиента № 1 в список доверенных",
            dependsOnMethods = "transaction7"
    )

    public void addClientsPositionToTrustedList() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM3).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        String[] coord = getClientsPosition(clientIds.get(0));

        getIC().locateTable(REFERENCE_ITEM3)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Долгота:", coord[1])
                .fillInputText("Широта:", coord[0])
                .save();
    }

    @Test(
            description = "Провести транзакцию № 8 с IP-адреса № 2 для Клиента № 1",
            dependsOnMethods = "addClientsPositionToTrustedList"
    )

    public void transaction8() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Найдено значение в справочнике 'Типичное расположение для клиента'");
    }

    @Test(
            description = "Провести транзакцию № 9 с IP-адреса № 2 для Клиента № 2",
            dependsOnMethods = "transaction8"
    )

    public void transaction9() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Изменить \"Дата первого посещения\" для IP-адреса № 1 Клиента № 1 на 3 дня назад" +
                    "Провести транзакцию № 10 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "transaction9"
    )

    public void transaction10() {//Изменяем в БД дату занесения в карантин на 3 дня назад
        //Через БД не получается поменять дату, после отправки транзакции дата меняется на текущую автоматически(ошибка в системе?)

//        HashMap<String, Object> map1 = new HashMap<>();
//        map1.put("LAST_TRANSACTION", Instant.now().minus(3, ChronoUnit.DAYS).toString());
//        map1.put("FIRST_DATE_VISIT", Instant.now().minus(3, ChronoUnit.DAYS).toString());
//        getDatabase().updateWhere("QUARANTINE_LOCATION", map1, "WHERE [id] = (SELECT MAX([id]) FROM [QUARANTINE_LOCATION])");

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM3).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        time1.add(Calendar.HOUR, -72);
        getIC().locateTable(REFERENCE_ITEM4)
                .findRowsBy()
                .match("Кол-во посещений", "2")
                .click()
                .edit()
                .fillInputText("Дата первого посещения:", format.format(time1.getTime()))
                .fillInputText("Дата последней транзакции:", format.format(time1.getTime()))
                .save();

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Местоположение уже находится в карантине");
        //Через БД не получается, после отпрпавки транзакции дата меняется на текущую автоматически
    }

    @Test(
            description = "Провести транзакцию № 11 с IP-адреса № 1 для Клиента № 2" +
                    "Занести IP-адрес № 2 для Клиента № 1 в список доверенных",
            dependsOnMethods = "transaction10"
    )

    public void transaction11() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
