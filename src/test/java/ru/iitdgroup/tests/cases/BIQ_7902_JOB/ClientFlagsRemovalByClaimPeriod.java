package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientFlagsRemovalByClaimPeriod extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}};
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM_SIM = "(System_parameters) Параметры переиспользования информации о замене SIM НМТ";
    private static final String WF_CLIENT_CHANGE = "Cмена учетных данных_true";//создан Action "Cмена учетных данных_true"
    private static final String WF_CLIENT_CHANGE_IMSI = "Изменен IMSI аутент_true";//создан Action "Изменен IMSI аутент_true"
    private static final Long version1 = 1L;

    @Test(
            description = "Включить правило: Отклонение 0.2; остальные правила деактивированы"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "CLAIM_PERIOD")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "IntegrDBO_SIM")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "IntegrOCRM_SIM")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "AutoInfoBlockSIM")
                .click()
                .edit()
                .fillInputText("Значение:", "3")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "AutoFullActiveSIM")
                .click()
                .edit()
                .fillInputText("Значение:", "3")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "AutoMailMessageSIMChange")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "AutoSMSMessageSIMConfirm")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM_SIM)
                .findRowsBy()
                .match("Код значения", "AutoMailMessageSIMConfirm")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateWorkflows()
                .openRecord("Клиент Workflow")
                .openAction(WF_CLIENT_CHANGE)
                .clearFieldMappings()
                .addFieldMapping("Смена учётных данных", "true", null)
                .addFieldMapping("Время смены учетных данных", "DateAdd(DAY,-4,Now())", null)
                .save();
        getIC().locateWorkflows()
                .openRecord("Клиент Workflow")
                .openAction(WF_CLIENT_CHANGE_IMSI)
                .clearFieldMappings()
                .addFieldMapping("Изменен IMSI телефона для аутентификации", "true", null)
                .addFieldMapping("Дата изменения IMSI телефона для аутентификации", "DateAdd(DAY,-4,Now())", null)
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
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
            description = "Провести Транзакцию № 1 с датой проведения 3 дня назад, Подтвердить её.",
            dependsOnMethods = "addClients"
    )

    public void transaction() {
        String transactionIdServ1;
        time.add(Calendar.HOUR, -72);
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionIdServ1 = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts().openFirst()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionIdServ1);
        assertTableField("Версия документа:", version1.toString());

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsClient(WF_CLIENT_CHANGE);
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsClient(WF_CLIENT_CHANGE_IMSI);

        assertTableField("Смена учётных данных:", "Yes");
        assertTableField("Изменен IMSI телефона для аутентификации:", "Yes");
    }

    @Test(
            description = "Запустить джоб ClientFlagsRemovalByClaimPeriod и проверить флаги у клиента ",
            dependsOnMethods = "transaction"
    )
    public void runJobStep2() {
        getIC().locateJobs()
                .selectJob("ClientFlagsRemovalByClaimPeriod")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        assertTableField("Смена учётных данных:", "No");
        assertTableField("Изменен IMSI телефона для аутентификации:", "No");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(version1)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }
}
