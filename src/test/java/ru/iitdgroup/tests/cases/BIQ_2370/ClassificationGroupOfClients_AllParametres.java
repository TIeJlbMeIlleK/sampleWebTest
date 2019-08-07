package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//TODO требуется доделать после исправления тикета BIQ2370-95

public class ClassificationGroupOfClients_AllParametres extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private static final String CLIENT_GROUP = "(Policy_parameters) Наименования групп клиентов";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";


    private final GregorianCalendar timeTransaction = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar time = new GregorianCalendar(1998, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2003, Calendar.DECEMBER, 12, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private final List<String> clientIds_2 = new ArrayList<>();
    private final List<String> clientIdsWithoutBirthday = new ArrayList<>();



    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {

//        FIXME требуется проверить после полной реализации доработки по группам клиетов
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 4; i++) {
                //FIXME Добавить проверку на существование клиента в базе

                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withBirthDate(new XMLGregorianCalendarImpl(time))
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
            description = "В справочник Наименования Групп клиентов добавлены 3 группы: Группа №1, Группа №2 и Группа по умолчанию",
            dependsOnMethods = "client"
    )
    public void editClientGroup(){
        Table.Formula rows = getIC().locateTable("(Policy_parameters) Признаки групп клиентов").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        Table.Formula rows1 = getIC().locateTable(CLIENT_GROUP).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_1")
                .save()
                .attach("Клиенты, добавленные вручную","Идентификатор клиента","Equals", clientIds.get(0));
//        FIXME требуется доработать

        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_2")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_3")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","FULL")
                .save();
    }

    @Test(
            description = "В справочнике \"Признаки групп клиентов\" указано для\n" +
                    "\"Группа №1\" -- Лица чей возраст = 20. \"Приоритет\" = 1\n" +
                    "\"Группа №2\" -- Лица, сумма транзакций у которых >= 2000. \"Приоритет\" = 2.\n" +
                    "\"Группа по умолчанию\" без дополнительных критериев, с установленым признаком \"Группа по умолчанию\"",
            dependsOnMethods = "editClientGroup"
    )
    public void editCriOfGroup(){

        Table.Formula rows = getIC().locateTable("(Policy_parameters) Признаки групп клиентов").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","Group_1")
                .fillInputText("Приоритет группы:","1")
                .fillInputText("Возраст клиента:","20")
                .save();

        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","Group_2")
                .fillInputText("Приоритет группы:","2")
                .fillInputText("Возраст клиента:","20")
                .fillInputText("Сумма транзакций (совершал):","2000")
                .fillInputText("Период времени (совершал):","7")
                .fillCheckBox("Признак  совершения транзакций Клиентом:",true)
                .save();

        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","Group_3")
                .fillInputText("Приоритет группы:","2")
                .fillCheckBox("Группа по умолчанию:", true)
                .save();

        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","FULL")
                .fillInputText("Приоритет группы:","5")
                .fillInputText("Время с момента подключения к ДБО ФЛ:","100")
                .fillInputText("Возраст клиента:","18")
                .fillInputText("Сумма транзакций (не совершал):","50000")
                .fillInputText("Период времени (не совершал):","7")
                .fillInputText("Сумма транзакций (совершал):","10000")
                .fillInputText("Период времени (совершал):","7")
                .fillCheckBox("Признак  совершения транзакций Клиентом:",true)
                .fillCheckBox("Признак не совершения транзакций Клиентом:",true)
                .fillCheckBox("Группа по умолчанию:",false)
                .save();
    }

    @Test(
            description = "Отправить транзакцию 1, любую, по Клиенту 1",
            dependsOnMethods = "editCriOfGroup"
    )
    public void transactionOfClient1(){
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getOuterTransfer().withAmountInSourceCurrency(new BigDecimal(2000.00));
        transactionData.getClientIds().withDboId(clientIds.get(0));
        sendAndAssert(transaction);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(0)).runReport()
                .openFirst();
        assertTableField("Группа клиента (ручное назначение):","Group_1");
        assertTableField("Предыдущая группа клиента:","");
    }

    @Test(
            description = "Отправить транзакцию 2, любую, по Клиенту 2 (Клиент не находится ни в одной из групп), для попадания в \"1 Группу\". Клиенту должно быть 20 лет.",
            dependsOnMethods = "transactionOfClient1"
    )
    public void transactionOfClient2(){

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(1));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(1000.00));
        sendAndAssert(transaction);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(1)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_1");
        assertTableField("Предыдущая группа клиента:","");

    }

    @Test(
            description = "Отправить транзакцию 2, любую, по Клиенту 2, для попадания во \"2 Группу\".",
            dependsOnMethods = "transactionOfClient1"
    )
    public void transactionOfClient2_1(){

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(1));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(1001.00));
        sendAndAssert(transaction);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(1)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_2");
        assertTableField("Предыдущая группа клиента:","Group_1");

    }

    @Test(
            description = "Отправить транзакцию 3,  критерии по транзакции не соответствуют ни одной из групп, по Клиенту 3",
            dependsOnMethods = "transactionOfClient2_1"
    )
    public void transactionOfClient3(){

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(2));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(2000.00));
        sendAndAssert(transaction);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(2)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_2");
        assertTableField("Предыдущая группа клиента:","");
    }

    @Test(
            description = "Отправить транзакцию 4, любую, по Клиенту 2, для попадания в  \"3 Группу\"",
            dependsOnMethods = "transactionOfClient3"
    )
    public void transactionOfClient4(){
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withBirthDate(new XMLGregorianCalendarImpl(time2))
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds_2.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }


        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds_2.get(0));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(500.00));
        sendAndAssert(transaction);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds_2.get(0)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_3");
        assertTableField("Предыдущая группа клиента:","");

    }

    @Test(
            description = "Отправить транзакцию 5, любую, по Клиенту 4 согласно всем критериям группы \"FULL\" из предусловия.",
            dependsOnMethods = "transactionOfClient4"
    )
    public void transactionOfClient5(){

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(3));
        transactionData.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(10000.00));
        sendAndAssert(transaction);
        Transaction transaction_2 = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData_2 = transaction.getData().getTransactionData();
        transactionData_2.getClientIds().withDboId(clientIds.get(3));
        transactionData_2.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(1000.00));
        sendAndAssert(transaction_2);
        Transaction transaction_3 = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData_3 = transaction.getData().getTransactionData();
        transactionData_3.getClientIds().withDboId(clientIds.get(3));
        transactionData_3.getOuterTransfer()
                .withAmountInSourceCurrency(new BigDecimal(1000.00));
        sendAndAssert(transaction_3);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(3)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","FULL");
        assertTableField("Предыдущая группа клиента:","");

        getIC().close();
        System.out.println("Тест кейс выполнен успешно!");

    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(timeTransaction))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(timeTransaction));
        return transaction;
    }

}