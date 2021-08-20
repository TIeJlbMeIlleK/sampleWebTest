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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_15_NonTypicalGeoPositionPayControl extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM2 = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Типичное расположение";
    private static final String REFERENCE_ITEM4 = "(Rule_tables) Карантин месторасположения";

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Павел", "Петушков", "Павлович"}};
    private static String[] login = {new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};

    //TODO создан тикет. Правило не работает с координатами. Проверить тест после исправления https://yt.iitdgroup.ru/issue/BIQ6046-105

    @Test(
            description = "Включаем и настраиваем правило и справочники" +
                    "Установить TIME_AFTER_ADDING_TO_QUARANTINE = 2"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .openRecord(RULE_NAME)
                .edit()
                .fillInputText("Точность сравнения координат:", "0,5")
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
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

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM3).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM3)
                .addRecord()
                .fillInputText("Долгота:", "41,0")
                .fillInputText("Широта:", "55,0")
                .fillInputText("Страна:", "Россия")
                .fillUser("Клиент:", clientIds.get(0))
                .save();

        Table.Formula rows1 = getIC().locateTable(REFERENCE_ITEM4).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        time1.add(Calendar.HOUR, -50);
        getIC().locateTable(REFERENCE_ITEM4)
                .addRecord()
                .fillInputText("Дата первого посещения:", format.format(time1.getTime()))
                .fillInputText("Дата последней транзакции:", format.format(time1.getTime()))
                .fillInputText("Долгота:", "45,1")
                .fillInputText("Широта:", "70,1")
                .fillInputText("Страна:", "Россия")
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();

        time2.add(Calendar.HOUR, -10);
        getIC().locateTable(REFERENCE_ITEM4)
                .addRecord()
                .fillInputText("Дата первого посещения:", format.format(time2.getTime()))
                .fillInputText("Дата последней транзакции:", format.format(time2.getTime()))
                .fillInputText("Долгота:", "35,1")
                .fillInputText("Широта:", "60,1")
                .fillInputText("Страна:", "Россия")
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1 Значения от PayControl: Широта: 55.1 Долгота: 40.8",
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
                .withIpAddress(null)
                .withLatitude("55,1")
                .withLongitude("40,8");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Найдено значение в справочнике 'Типичное расположение для клиента'");
    }

    @Test(
            description = "Отправить транзакцию №2 от клиента №1 Значения от PayControl: Широта: 55.1 Долгота: 41.6",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(null)
                .withLatitude("55,1")
                .withLongitude("41,6");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1 Значения от PayControl: Широта: 70.1 Долгота: 45.1",
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
                .withIpAddress(null)
                .withLatitude("70,1")
                .withLongitude("45,1");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION_FOR_IP);
    }

    @Test(
            description = "Отправить транзакцию №4 от клиента №1 Значения от PayControl: Широта: 45.1 Долгота: 70.1",
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
                .getClientDevice()
                .getIOS()
                .withIpAddress(null)
                .withLatitude("45,1")
                .withLongitude("70,1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Отправить транзакцию №5 от клиента №1 Значения от PayControl: Широта: 60.1 Долгота: 35.1",
            dependsOnMethods = "transaction4"
    )

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
                .withIpAddress(null)
                .withLatitude("60,1")
                .withLongitude("35,1");
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
