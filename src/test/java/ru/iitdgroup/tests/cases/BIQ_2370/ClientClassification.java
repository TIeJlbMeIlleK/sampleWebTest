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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ClientClassification extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private static final String CLIENT_GROUP = "(Policy_parameters) Наименования групп клиентов";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";

    private final GregorianCalendar time = new GregorianCalendar(1999, Calendar.JULY, 10, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();
    private final List<String> clientIdsWithoutBirthday = new ArrayList<>();



    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);
    }

    @Test(
            description = "Настроить WF для попадания первой транзакции на РДАК",
            dependsOnMethods = "enableRules"
    )
        public void editClientGroup(){

        Table.Formula rows = getIC().locateTable(CLIENT_GROUP).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_1")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_2")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_3")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_4")
                .save();
    }

    @Test(
            description = "Настроить справочник Признаки групп клиентов",
            dependsOnMethods = "editClientGroup"
    )
    public void editCriOfGroup(){

        Table.Formula rows = getIC().locateTable("(Policy_parameters) Признаки групп клиентов").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Имя группы:","Group_3")
                .fillInputText("Приоритет группы:","2")
                .fillInputText("Возраст клиента:","20")
                .save();

        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Имя группы:","Group_4")
                .fillInputText("Приоритет группы:","1")
                .fillInputText("Возраст клиента:","20")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editCriOfGroup"
    )
    public void client() {
        try {
            for (int i = 0; i < 5; i++) {
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
            description = "Создаем клиента без указания дня рождения",
            dependsOnMethods = "client"
    )
    public void clientWithoutBirthday() {
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
                client.getData().getClientData().getClient().setBirthDate(null);
                sendAndAssert(client);
                clientIdsWithoutBirthday.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Перейти в справочник Наименование групп клиентов Перейти в Группу №1, добавить Клиента №1 в Клиенты, добавленные вручную, с помощью кнопки Attach Перейти в Группу №2, добавить Клиента №1 в Клиенты, добавленные автоматически, с помощью кнопки Attach",
            dependsOnMethods = "editClientGroup"
    )
    public void workWithClient1And2(){

        getIC().locateTable(CLIENT_GROUP).findRowsBy().match("Имя группы","Group_1");


        getIC().locateTable(CLIENT_GROUP).findRowsBy().match("Имя группы","Group_1");

//        TODO требуется дописать
    }

    @Test(
            description = "Отправить транзакцию от клиента №2 с идентичными критериями для попадания в Группу 3 и Группу 4",
            dependsOnMethods = "workWithClient1And2"
    )
    public void workWithClient3And4(){

        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //        TODO требуется дописать проверку отнесения Клиента №2 в нужную нам группу
    }
    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}