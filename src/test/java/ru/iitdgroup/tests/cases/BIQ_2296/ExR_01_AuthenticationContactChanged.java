package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.dbdriver.Database;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class ExR_01_AuthenticationContactChanged extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_01_AuthenticationContactChanged";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
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
            description = "Отредактировать клиентов",
            dependsOnMethods = "client"
    )

    public void editClientOne() {

//        TODO КЛИЕНТ без флагов
        Map<String, Object> values = new HashMap<>();
        values.put("CHANGE_AUTH_IMSI", 0);
        values.put("CHANGE_CLIENT_CONTACT", 0);

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.Client", values, "WHERE DBO_ID ="+ clientIds.get(0));
        } catch (Exception e) {
            e.printStackTrace();
        }

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(0)).runReport()
                .openFirst();
        assertTableField("Смена учётных данных:","No");
        assertTableField("Изменен IMSI телефона для аутентификации:","No");
    }

    @Test(
            description = "Отредактировать клиентов",
            dependsOnMethods = "editClientOne"
    )
    public void editClientTwo() {
//        TODO КЛИЕНТ с CHANGE_CLIENT_CONTACT
        Map<String, Object> values = new HashMap<>();
        values.put("CHANGE_AUTH_IMSI", 0);
        values.put("CHANGE_CLIENT_CONTACT", 1);

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.Client", values, "WHERE DBO_ID ="+ clientIds.get(1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(1)).runReport()
                .openFirst();
        assertTableField("Смена учётных данных:","Yes");
        assertTableField("Изменен IMSI телефона для аутентификации:","No");
    }

    @Test(
            description = "Отредактировать клиентов",
            dependsOnMethods = "editClientTwo"
    )
    public void editClientThree(){
//        TODO КЛИЕНТ с CHANGE_AUTH_IMSI
        Map<String, Object> values = new HashMap<>();
        values.put("CHANGE_AUTH_IMSI", 1);
        values.put("CHANGE_CLIENT_CONTACT", 0);
        getDatabase().updateWhere("dbo.Client", values, "WHERE DBO_ID ="+ clientIds.get(2));

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(2)).runReport()
                .openFirst();
        assertTableField("Смена учётных данных:","No");
        assertTableField("Изменен IMSI телефона для аутентификации:","Yes");
    }

    @Test(
            description = "Отредактировать клиентов",
            dependsOnMethods = "editClientThree"
    )
    public void editClientFour(){
//        TODO КЛИЕНТ с CHANGE_CLIENT_CONTACT и CHANGE_AUTH_IMSI
        Map<String, Object> values = new HashMap<>();
        values.put("CHANGE_AUTH_IMSI", 1);
        values.put("CHANGE_CLIENT_CONTACT", 1);
        getDatabase().updateWhere("dbo.Client", values, "WHERE id="+ clientIds.get(3));

        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(3)).runReport()
                .openFirst();
        assertTableField("Смена учётных данных:","Yes");
        assertTableField("Изменен IMSI телефона для аутентификации:","Yes");
    }

    @Test(
            description = "Провести люблю транзакцию № 1 от имени клиента № 1, у которого не взведены флаги Смена учетных данных и Изменен IMSI телефона для аутентификации",
            dependsOnMethods = "editClientFour"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 2 на реквизиты получателя, где БИК и СЧЕТ в запрещенных, а Сводный ФИО получателя - нет",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .setOperationDescription("пополнение карты 2200021030605190 Петров Дмитрий Сергеевич");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011555");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805111");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }
    @Test(
            description = "Провести транзакцию № 3 на реквизиты получателя, где БИК и Сводный ФИО получателя в запрещенных, а СЧЕТ - нет",
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
                .getOuterTransfer()
                .setOperationDescription("пополнение карты 2200021030605190 Иванов Иван Иванович");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011678");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805111");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }
    @Test(
            description = "Провести транзакцию № 4 на реквизиты получателя, где Сводный ФИО получателя и СЧЕТ в запрещенных, а БИК - нет",
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
                .getOuterTransfer()
                .setOperationDescription("пополнение карты 2200021030605190 Иванов Иван Иванович");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011555");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805555");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }



    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
