package ru.iitdgroup.tests.cases.BIQ_TEST;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class ExR_01_AuthenticationContactChanged extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_ExR_01_AuthenticationContactChanged";
    private static final String TABLE = "Список клиентов";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
//    TODO перед выполнением ТК требуется создание Action в ClientWF для взведения влагов "Изменение ИМСИ" и "Смена учетных данных"

    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
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

//    @Test(
//            description = "Включаем правило и взводим флаги по клиентам",
//            dependsOnMethods = "createClients"
//    )
//    public void step0() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .editRule(RULE_NAME)
//                .fillCheckBox("Active:",true)
//                .save()
//                .sleep(15);
//
//        getIC().locateReports()
//                .openFolder("Бизнес-сущности")
//                .openRecord(TABLE)
//                .setTableFilterWithActive("Идентификатор клиента","Equals",clientIds.get(1))
//                .runReport()
//                .openFirst()
//                .getActionsForClient()
//                .doAction("Изменен IMSI аутент_true")
//                .approved();
//
//        getIC().locateReports()
//                .openFolder("Бизнес-сущности")
//                .openRecord(TABLE)
//                .setTableFilterWithActive("Идентификатор клиента","Equals",clientIds.get(2))
//                .runReport()
//                .openFirst()
//                .getActionsForClient()
//                .doAction("Cмена учетных данных_true")
//                .approved();
//        getIC().close();
//
//    }
//
//    @Test(
//            description = "Провести транзакцию № 1 от имени клиента № 1, у которого не взведены флаги \"Смена учетных данных\" и \"Изменен IMSI телефона для аутентификации\"",
//            dependsOnMethods = "step0"
//    )
//
//    public void step1() {
//        Transaction transaction = getTransaction();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(0));
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
//    }
//
//    @Test(
//            description = "Провести транзакцию № 2 от имени клиента № 2, у которого  взведен только флаг \"Смена учетных данных\"",
//            dependsOnMethods = "step1"
//    )
//    public void step2() {
//        Transaction transaction = getTransaction();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(2));
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(TRIGGERED, RESULT_CHANGE_CONTACT);
//    }
//
//    @Test(
//            description = "Провести транзакцию № 3 от имени клиента № 3, у которого  взведен только флаг \"Изменен IMSI телефона для аутентификации\"",
//            dependsOnMethods = "step2"
//    )
//    public void step3() {
//        Transaction transaction = getTransaction();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(1));
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(TRIGGERED, RESULT_CHANGE_IMSI);
//    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
