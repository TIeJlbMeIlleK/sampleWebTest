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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientClassification extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";
    private static final String CLIENT_GROUP = "(Policy_parameters) Наименования групп клиентов";
    private final GregorianCalendar time = new GregorianCalendar(1998, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar timeTransaction = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2005, Calendar.DECEMBER, 12);
    private final List<String> clientIds = new ArrayList<>();
    private final List<String> clientIdsWithoutBirthday = new ArrayList<>();


    @Test(
            description = "Настройка и выключение правил"
    )
    public void enableRules() {
        System.out.println("\"Клиент может быть только в одной группе,\n" +
                "Клиент, добавленный в группу вручную, не может быть переназначен в другую группу,\n" +
                "При наличии двух групп с одними условиями, но с разным приоритетом, выставляется клиенту та группа, которая имеет высший приоритет,\n" +
                "если у клиента пустое поле, то признак не проверяется. Если группа \"По умолчанию\" не существует, и клиент не подпадает ни под одну группу, то у него поле группа будет пустое. -- BIQ2370" + " ТК№11");

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
            description = "Создаем клиента без указания дня рождения",
            dependsOnMethods = "client"
    )
    public void clientNot18() {
        try {
            for (int i = 0; i < 1; i++) {
                //FIXME Добавить проверку на существование клиента в базе
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
                clientIdsWithoutBirthday.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Создать 4 группы по клиентам",
            dependsOnMethods = "clientNot18"
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
                .attach("Клиенты, добавленные автоматически","Идентификатор клиента","Equals", clientIds.get(0))
                .attach("Клиенты, добавленные вручную","Идентификатор клиента","Equals", clientIds.get(0));
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_2")
                .save()
                .attach("Клиенты, добавленные автоматически","Идентификатор клиента","Equals", clientIds.get(1));
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_3")
                .save();
        getIC().locateTable(CLIENT_GROUP).addRecord().fillInputText("Имя группы:","Group_4")
                .save();
    }

    @Test(
            description = "Для Группы 3 указать Приоритет группы = 2 и Возраст = 20 Для Группы 4 указать Приоритет группы = 1 и Возраст = 20",
            dependsOnMethods = "editClientGroup"
    )
    public void editCriteriesOfGroup(){
        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","Group_3")
                .fillInputText("Приоритет группы:","2")
                .fillInputText("Возраст клиента:","20")
                .save();

        getIC().locateTable("(Policy_parameters) Признаки групп клиентов").addRecord().fillInputText("Наименование группы:","Group_4")
                .fillInputText("Приоритет группы:","1")
                .fillInputText("Возраст клиента:","20")
                .save();
    }

    @Test(
            description = "Перейти в справочник Наименование групп клиентов Перейти в Группу №1, добавить Клиента №1 в " +
                    "Клиенты, добавленные вручную, с помощью кнопки Attach Перейти в Группу №2, добавить Клиента №1 в Клиенты, " +
                    "добавленные автоматически, с помощью кнопки Attach",
            dependsOnMethods = "editCriteriesOfGroup"
    )
    public void workWithClient1And2(){

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(0)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","");
        assertTableField("Группа клиента (ручное назначение):","Group_1");
        assertTableField("Предыдущая группа клиента:","");


        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(1)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_2");
        assertTableField("Группа клиента (ручное назначение):","");
        assertTableField("Предыдущая группа клиента:","");
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
                .withDboId(clientIds.get(2));
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(2)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","Group_3");

    }

    @Test(
            description = "Отправить транзакцию от клиента без указанного дня рождения с идентичными критериями для попадания в Группу 3 и Группу 4",
            dependsOnMethods = "workWithClient3And4"
    )
    public void transactionOfClientNo18YO(){

        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIdsWithoutBirthday.get(0));
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIdsWithoutBirthday.get(0)).runReport()
                .openFirst();
        assertTableField("Группа клиента (автоматическое назначение):","");
        assertTableField("Предыдущая группа клиента:","");
        assertTableField("Группа клиента (ручное назначение):","");

    }
    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(timeTransaction))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(timeTransaction));
        return transaction;
    }

}