package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_01_AuthenticationContactChanged extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_01_AuthenticationContactChanged";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ирина", "Дьякова", "Витальевна"}};

    @Test(
            description = "Настройка и включение правила R01_ExR_01_AuthenticationContactChanged"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
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
    }
//TODO перед запуском должен быть создан WF Action, если не создан, то запустить следующий тест enableFlags

//    @Test(
//            description = "Создаем ACTION для изменения флагов по клиенту",
//            dependsOnMethods = "client"
//    )
//    public void enableFlags() {
//
//        getIC().locateWorkflows()
//                .openRecord("Клиент Workflow")
//                .addAction()
//                .setDisplayName("CHANGE_CLIENT_CONTACT_TRUE")
//                .addFromState(WorkflowAction.WorkflowActionState.ANY_STATE)
//                .addToState(WorkflowAction.WorkflowActionState.KEEP_CURRENT_STATE)
//                .addFieldMapping("Смена учётных данных", "true", null)
//                .save()
//                .addAction()
//                .setDisplayName("CHANGE_CLIENT_CONTACT_FALSE")
//                .addFromState(WorkflowAction.WorkflowActionState.ANY_STATE)
//                .addToState(WorkflowAction.WorkflowActionState.KEEP_CURRENT_STATE)
//                .addFieldMapping("Смена учётных данных", "false", null)
//                .save()
//                .addAction()
//                .setDisplayName("CHANGE_AUTH_IMSI_FALSE")
//                .addFromState(WorkflowAction.WorkflowActionState.ANY_STATE)
//                .addToState(WorkflowAction.WorkflowActionState.KEEP_CURRENT_STATE)
//                .addFieldMapping("Изменен IMSI телефона для аутентификации", "false", null)
//                .save()
//                .addAction()
//                .setDisplayName("CHANGE_AUTH_IMSI_TRUE")
//                .addFromState(WorkflowAction.WorkflowActionState.ANY_STATE)
//                .addToState(WorkflowAction.WorkflowActionState.KEEP_CURRENT_STATE)
//                .addFieldMapping("Изменен IMSI телефона для аутентификации", "true", null)
//                .save();
//    }

    @Test(
            description = "1. Провести транзакцию № 1 \"Заявка на выпуск карты\" от имени клиента № 1, " +
                    "у которого не взведены флаги \"Смена учетных данных\" и \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "client"
    )

    public void transaction1() {

//        TODO КЛИЕНТ без флагов
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Заявка на выпуск карты\" " +
                    "от имени клиента № 2, у которого  взведен только флаг \"Смена учетных данных\"",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
//        TODO КЛИЕНТ с CHANGE_CLIENT_CONTACT

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0)).runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("CHANGE_CLIENT_CONTACT_TRUE")
                .approved();
        assertTableField("Изменен IMSI телефона для аутентификации:", "No");
        assertTableField("Смена учётных данных:", "Yes");

        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Изменен контакт клиента для аутентификации");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Заявка на выпуск карты\" от имени клиента № 3, " +
                    "у которого  взведен только флаг \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0)).runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("CHANGE_CLIENT_CONTACT_FALSE")
                .approved();
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0)).runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("CHANGE_AUTH_IMSI_TRUE")
                .approved();
        assertTableField("Изменен IMSI телефона для аутентификации:", "Yes");
        assertTableField("Смена учётных данных:", "No");

        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Изменен IMSI телефона для аутентификации");
    }

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
