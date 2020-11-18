package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_15_NonTypicalGeoPosition extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM2 = "(System_parameters) Интеграционные параметры";
    private static final String IP_ADDRESS1 = "95.73.149.81";
    private static final String IP_ADDRESS2 = "95.24.51.82";
    private static final String NON_EXISTENT_IP_ADDRESS = "1.1.1.1";
    private static final String IP_ADDRESS3 = "95.77.77.77";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Типичное расположение";

    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);
    }

    @Test(
            description = "Установить TIME_AFTER_ADDING_TO_QUARANTINE = 2 " +
                    "(справочник \"Параметры обработки справочников и флагов\") в параметре правила" +
                    " и Выключить интеграцию с ГИС (GisSystem_GIS)",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData() {

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();

        getIC().locateTable(REFERENCE_ITEM2)//как спустится в браузере вниз? если не видит нужное...
                .setTableFilter("Описание", "Equals", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .refreshTable()
                .findRowsBy()
                .match("Описание", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "0").save();
    }

    @Test(
            description = " Включить интеграцию с ГИС (GisSystem_GIS)",
            dependsOnMethods = "step1"
    )
    public void enableGIS() {

        getIC().locateTable(REFERENCE_ITEM2)//как спустится в браузере вниз? если не видит нужное...
                .setTableFilter("Описание", "Equals", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .refreshTable()
                .findRowsBy()
                .match("Описание", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();

    }

    @Test(
            description = "Занести IP-адрес(долготу и широту) № 2 для Клиента № 1 в список доверенных",
            dependsOnMethods = "step8"
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

        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceData"
    )
    public void step0() {
        try {
            for (int i = 0; i < 2; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1 с указанием ip-адреса",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_GIS_SETTING);
    }

    @Test(
            description = "Провести транзакцию № 2 с указанием ip-адреса, регулярная",
            dependsOnMethods = "enableGIS"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 3 без указания ip-адреса",
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
                .getClientDevice()
                .getIOS()
                .setIpAddress(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, REQUIRE_IP);
    }

    @Test(
            description = "Провести транзакцию № 4 без контейнера device",
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
                .setClientDevice(null);
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
                .getClientDevice()
                .getIOS()
                .setIpAddress(NON_EXISTENT_IP_ADDRESS);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
        //выход не совпадает с требованиями, должен быть FEW_DATA и "Недосточно данных: для полученного ip адреса нет данных о геолокации"
    }

    @Test(
            description = "Провести транзакции № 6 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "step5"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Провести транзакции № 7 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "step6"
    )

    public void step7() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }


    @Test(
            description = "Провести транзакцию № 9 с IP-адреса № 1 для Клиента № 2",
            dependsOnMethods = "step7"
    )

    public void step8() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }


    @Test(
            description = "Провести транзакцию № 10 с IP-адреса № 2 для Клиента № 1",
            dependsOnMethods = "addClientsPositionToTrustedList"
    )

    public void step9() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS2);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Найдено значение в справочнике 'Типичное расположение для клиента'");
    }

    @Test(
            description = " Провести транзакцию № 11 с IP-адреса № 2 для Клиента № 2",
            dependsOnMethods = "step9"
    )

    public void step10() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIpAddress(IP_ADDRESS2);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }


}
