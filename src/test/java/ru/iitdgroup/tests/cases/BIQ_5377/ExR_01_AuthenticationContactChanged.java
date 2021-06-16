package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_01_AuthenticationContactChanged extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_01_AuthenticationContactChanged";
    private static final String WF_CLIENT_CHANGE = "Cмена учетных данных_true";
    private static final String WF_CLIENT_CHANGE_IMSI = "Изменен IMSI аутент_true";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ирина", "Дьякова", "Витальевна"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);

    @Test(
            description = "Настройка и включение правила R01_ExR_01_AuthenticationContactChanged"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);

        getIC().locateWorkflows()
                .openRecord("Клиент Workflow")
                .openAction(WF_CLIENT_CHANGE)
                .clearFieldMappings()
                .addFieldMapping("Смена учётных данных", "true", null)
                .save();
        getIC().locateWorkflows()
                .openRecord("Клиент Workflow")
                .openAction(WF_CLIENT_CHANGE_IMSI)
                .clearFieldMappings()
                .addFieldMapping("Изменен IMSI телефона для аутентификации", "true", null)
                .save();

        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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

    @Test(
            description = "1. Провести транзакцию № 1 \"Платеж по QR-коду через СБП\" от имени клиента № 1," +
                    " у которого не взведены флаги \"Смена учетных данных\" и \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "client"
    )

    public void transaction1() {

//        TODO КЛИЕНТ без флагов
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Платеж по QR-коду через СБП\" от имени клиента № 2, у которого  взведен только флаг \"Смена учетных данных\"",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
//        TODO КЛИЕНТ с CHANGE_CLIENT_CONTACT

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction(WF_CLIENT_CHANGE)
                .approved();
        assertTableField("Изменен IMSI телефона для аутентификации:", "No");
        assertTableField("Смена учётных данных:", "Yes");

        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Изменен контакт клиента для аутентификации");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Платеж по QR-коду через СБП\" от имени клиента № 3, " +
                    "у которого  взведен только флаг \"Изменен IMSI телефона для аутентификации\"",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
//        TODO КЛИЕНТ с CHANGE_AUTH_IMSI

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("Cмена учетных данных_false")
                .approved();
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction(WF_CLIENT_CHANGE_IMSI)
                .approved();
        assertTableField("Изменен IMSI телефона для аутентификации:", "Yes");
        assertTableField("Смена учётных данных:", "No");

        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Изменен IMSI телефона для аутентификации");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "transaction3"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}
