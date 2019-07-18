package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.ves.mock.VesMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RDAKParametrisation extends RSHBCaseTest {

    private static final String TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";
    public static final String TABLE_2 = "(System_parameters) Параметры TimeOut";
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String WEEKENDDAYS = "(Rule_tables) Производственный календарь";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";

    private final GregorianCalendar time1 = new GregorianCalendar(2019, Calendar.JULY, 1, 9, 30, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2019, Calendar.JULY, 1, 9, 0, 0);
    private final GregorianCalendar time3 = new GregorianCalendar(2019, Calendar.JULY, 1, 18, 36, 0);
    private final GregorianCalendar time4 = new GregorianCalendar(2019, Calendar.JULY, 6, 10, 0, 0);
    private final GregorianCalendar time5 = new GregorianCalendar(2019, Calendar.JULY, 8, 10, 0, 0);
    private final GregorianCalendar time6 = new GregorianCalendar(2019, Calendar.JULY, 15, 22, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка справочника Параметры обработки справочников и флагов"
    )
    public void editTables() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Час начала рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "9")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Час окончания рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "18")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Минута начала рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "30")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Минута окончания рабочего дня")
                .click()
                .edit()
                .fillInputText("Значение:", "30")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Описание", "Кол-во рабочих дней на которые Банк имеет право приостановить исполнение транзакции клиента")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
    }

    @Test(
            description = "Настройка справочника Параметры обработки справочников и флагов",
            dependsOnMethods = "editTables"
    )
    public void editTimeOut() {
        getIC().locateTable(TABLE_2).addRecord()
                .fillCheckBox("Календарь:", true)
                .fillInputText("Наименование канала ДБО:","Интернет клиент")
                .fillInputText("Тип транзакции:","Оплата услуг")
                .fillInputText("Время (мс):","3600000");
    }

    @Test(
            description = "Включкние стороннего правила для создания Алерта по транзакции",
            dependsOnMethods = "editTimeOut"
    )
    public void enableRuleForAlert() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Включкние стороннего правила для создания Алерта по транзакции",
            dependsOnMethods = "enableRuleForAlert"
    )
    public void refactorWorkFlow() {
        //TODO добавить настройку WF



        Table.Formula rows = getIC().locateTable(RDAK).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","rdak_underfire")
                .fillInputText("Новый статус:","RDAK_Done").save();
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","Wait_RDAK")
                .fillInputText("Новый статус:","RDAK_Done").save();
    }

    @Test(
            description = "Включкние стороннего правила для создания Алерта по транзакции",
            dependsOnMethods = "refactorWorkFlow"
    )
    public void editWeekendDays() {



        getIC().locateTable(WEEKENDDAYS)
                .addRecord()
                .fillInputText("Выходной день:","15.07.2019").save();
        getIC().locateTable(WEEKENDDAYS)
                .addRecord()
                .fillInputText("Выходной день:","17.07.2019").save();
        getIC().locateTable(WEEKENDDAYS)
                .addRecord()
                .fillInputText("Выходной день:","08.07.2019").save();
        getIC().close();

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editWeekendDays"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Произвести транзакцию 1 Перевод на карту другому лицу от Клиента 1, сумма 500",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_1();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        //TODO нужно реализовать проверку срока РДАК для данной транзакции и параметра SUSPEND
    }

    @Test(
            description = "Произвести транзакцию 2 Перевод на карту другому лицу от Клиента 1, сумма 500",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_2();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);

    }

    @Test(
            description = "Произвести транзакцию 3 Оплата услуг от Клиента 1, сумма 1000",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_3();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);

    }

    @Test(
            description = "Произвести транзакцию 4 Оплата услуг от Клиента 2, сумма 500",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_4();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);

    }

    @Test(
            description = "Произвести транзакцию 5 Оплата услуг от Клиента 2, сумма 500",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_5();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);

    }
    @Test(
            description = "Произвести транзакцию 6 Перевод на счет от Клиента 2, сумма 1000",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_6();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);

    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT_1() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1));
        return transaction;
    }

    private Transaction getTransactionSERVICE_PAYMENT_2() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2));
        return transaction;
    }

    private Transaction getTransactionSERVICE_PAYMENT_3() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time3))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time3));
        return transaction;
    }
    private Transaction getTransactionSERVICE_PAYMENT_4() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time4))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time4));
        return transaction;
    }
    private Transaction getTransactionSERVICE_PAYMENT_5() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time5))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time5));
        return transaction;
    }
    private Transaction getTransactionSERVICE_PAYMENT_6() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time6))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time6));
        return transaction;
    }

    private static VesMock getVesMock() {
        return VesMock.create().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");
    }
}
