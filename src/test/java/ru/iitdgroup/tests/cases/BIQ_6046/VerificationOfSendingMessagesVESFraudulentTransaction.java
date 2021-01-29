package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Record;
import ru.iitdgroup.tests.webdriver.referencetable.Table;
import org.openqa.selenium.TimeoutException;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction;
import ru.iitdgroup.tests.webdriver.report.ReportRecord;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.*;


public class VerificationOfSendingMessagesVESFraudulentTransaction extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_07_Devices";
    private static final String REFERENCE_ITEM1 = "(System_parameters) Интеграционные параметры";
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String SESSION_ID = "555krl345";
    private static final String UNIQUENAME = "SEND_FROM_VES1";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Лариса", "Каримова", "Игоревна"}, {"Ольга", "Петрова", "Ивановна"}};
    private String workflowRecordUniqueName;
    private String lastVESFeedbackIdBeforeTest;


    @Test(
            description = "Создать Action в TransactionWF с указанием ExternalApi на отправку сообщения в ВЭС по мошеннической транзакции (Send_from_VES).\n" +
                    "Condition: Transaction_object.status == 'Complete' AND\n" +
                    "Transaction_object.resolution == 'Fraud'\n" +
                    "FieldMapping: fraudReason = \"Указать причину признания транзакции мошеннической\""
    )

    public void createActionInTransactionWF() {

        WorkflowAction record = getIC()
                .locateWorkflows()
                .openRecord("Транзакция Workflow")
                .addAction()
                .addFromState(WorkflowAction.WorkflowActionState.ANY_STATE)
                .addToState(WorkflowAction.WorkflowActionState.PROCESSED, WorkflowAction.WorkflowActionResolution.FRAUD)
                .setCondition("Transaction_object.status == 'Complete' AND\nTransaction_object.resolution == 'Fraud'")
                .addFieldMapping("Причина признания транзакции мошеннической", "\"Мошенничество\"", null)
                .setCustomExternalAPIsSendVESfeedback();

        String recordUniqueName = UNIQUENAME;
        Boolean savedSuccessfully = false;
        while (!savedSuccessfully) {
            try {
                record.setDisplayName(recordUniqueName);
                record.getUniqueName();
                record.setUniqueName(recordUniqueName);
                record.save();
                savedSuccessfully = true;
            } catch (TimeoutException e) { //если сохранение не удалось и такой unique name уже существует
                recordUniqueName = recordUniqueName + "1";
            }
        }
        workflowRecordUniqueName = recordUniqueName;
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "createActionInTransactionWF"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "отправка сообщения ВЭС с Раббита на 2 клиентов",
            dependsOnMethods = "addClient"
    )

    public void sendResponseFromVES() {

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("login", LOGIN);
            json.put("login_hash", LOGIN_HASH);
            json.put("session_id", SESSION_ID);
            json.put("device_hash", SESSION_ID);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

            json.put("login", LOGIN + "1");
            json.put("login_hash", LOGIN_HASH + "1");
            json.put("session_id", SESSION_ID + "1");
            json.put("device_hash", SESSION_ID + "1");
            newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Включить любое правило для формирования Алерта",
            dependsOnMethods = "sendResponseFromVES"
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
            description = "Включить: 1. VES_FFEDBACK = 1\n" +
                    "2. Включить LOG_VES\n" +
                    "3. IntegrVES2 = 1",
            dependsOnMethods = "enableRules"
    )
    public void enableVES2() {

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Описание", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Описание", "Интеграция с ВЭС по отправке данных. Если параметр включен – отправка данных выполняется.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Описание", "Логирование сообщений от VES (in/out)")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save()
                .sleep(5);
    }

    @Test(
            description = "В логированных сообщениях для будующей проверки " +
                    "сохраняем последний Идентификатор,поступивший от VES_FEEDBACKt",
            dependsOnMethods = "enableVES2"
    )
    public void saveLastVESFeedbackId() {
        lastVESFeedbackIdBeforeTest = getIC()
                .locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilter("Тип сервиса", "Equals", "VES_FEEDBACK")
                .getLastRecordIdentificator();

    }

    @Test(
            description = "Выключить: VES_FFEDBACK = 0",
            dependsOnMethods = "goToLoggedMessages1"
    )
    public void turnOffVES2() {

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Описание", "Интеграция с ВЭС по отправке данных. Если параметр включен – отправка данных выполняется.")
                .click()
                .edit()
                .fillInputText("Значение:", "0")
                .save()
                .sleep(5);
    }

    @Test(
            description = " Отправить транзакцию № 1 от клиента №1 ",
            dependsOnMethods = "saveLastVESFeedbackId"
    )

    public void step0() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        transactionData.getClientIds().withLoginHash(LOGIN_HASH);
        transactionData.withSessionId(SESSION_ID);

        sendAndAssert(transaction);
    }

    @Test(
            description = "Отправить транзакцию № 2 от клиента №1 ",
            dependsOnMethods = "turnOffVES2"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientIds().withLoginHash(LOGIN_HASH + "1");
        transactionData.withSessionId(SESSION_ID + "1");

        sendAndAssert(transaction);
    }

    @Test(
            description = "Перейти в сформированный алерт и проставить Статус = Обработано, Резолюция = Мошенничество и" +
                    "Перейти в транзакцию №1 и выполнить Action Send_from_VES",
            dependsOnMethods = "step0"
    )

    public void goToAletrt1() {
        getIC()
                .locateAlerts()
                .openFirst()
                .action("Мошенничество")
                .goToTransactionPage()
                .action(workflowRecordUniqueName)
                .sleep(3);
    }

    @Test(
            description = "Перейти в сформированный алерт и проставить Статус = Обработано, Резолюция = Мошенничество и" +
                    "Перейти в транзакцию №2 и выполнить Action Send_from_VES",
            dependsOnMethods = "step1"
    )

    public void goToAletrt2() {
        getIC()
                .locateAlerts()
                .openFirst()
                .action("Мошенничество")
                .goToTransactionPage()
                .action(workflowRecordUniqueName)
                .sleep(3);
    }

    @Test(
            description = "Проверить логированные сообщения на отправку сообщения ВЭС",
            dependsOnMethods = "goToAletrt1"
    )

    public void goToLoggedMessages1() {
        ReportRecord recordsPage = getIC()
                .locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilter("Тип сервиса", "Equals", "VES_FEEDBACK");
        String lastId = recordsPage
                .getLastRecordIdentificator();

        assertNotSame(lastVESFeedbackIdBeforeTest, lastId);
        lastVESFeedbackIdBeforeTest = lastId;

        String success = recordsPage.getLastRecordSuccess();
        assertEquals(success, "Yes");
    }

    @Test(
            description = "Проверить логированные сообщения на отправку сообщения ВЭС",
            dependsOnMethods = "goToAletrt2"
    )

    public void goToLoggedMessages() {
        ReportRecord recordsPage = getIC()
                .locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilter("Тип сервиса", "Equals", "VES_FEEDBACK");
        String lastId = recordsPage
                .getLastRecordIdentificator();

        assertEquals(lastVESFeedbackIdBeforeTest, lastId);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
