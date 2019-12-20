package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.ignite.IgniteMessaging;
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


public class ExR_01_Transaction extends RSHBCaseTest {
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

    public void editClients() {

//        TODO КЛИЕНТ без флагов
        Map<String, Object> values = new HashMap<>();
        values.put("CHANGE_AUTH_IMSI", 0);
        values.put("CHANGE_CLIENT_CONTACT", 1);
        try (Database db = getDatabase()) {
            db.updateWhere("dbo.Client", values, "WHERE DBO_ID ="+ clientIds.get(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> valuesForClient2 = new HashMap<>();
        valuesForClient2.put("CHANGE_AUTH_IMSI", 1);
        valuesForClient2.put("CHANGE_CLIENT_CONTACT", 0);

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.Client", valuesForClient2, "WHERE DBO_ID ="+ clientIds.get(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> valuesForClient3 = new HashMap<>();
        valuesForClient3.put("CHANGE_AUTH_IMSI", 1);
        valuesForClient3.put("CHANGE_CLIENT_CONTACT", 1);

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.Client", valuesForClient3, "WHERE DBO_ID ="+ clientIds.get(3));
        } catch (Exception e) {
            e.printStackTrace();
        }
        IgniteMessaging rmtMsg = getMsg();
        rmtMsg.send("RELOAD_CLIENT", this.getClass().getSimpleName());

    }

    @Test(
            description = "Провести люблю транзакцию № 1 от имени клиента № 1, у которого не взведены флаги \"Смена учетных данных\" и \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "editClients"
    )
    public void step1() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести люблю транзакцию № 2 от имени клиента № 2, у которого  взведен только флаг \"Смена учетных данных\"",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_CHANGE_CONTACT);
    }
    @Test(
            description = "Провести люблю транзакцию № 3 от имени клиента № 3, у которого  взведен только флаг \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_CHANGE_IMSI);
    }
    @Test(
            description = "Провести люблю транзакцию № 4 от имени клиента № 4, у которого взведены флаги \"Смена учетных данных\" и \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_CHANGE_CONTACT);
    }



    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
