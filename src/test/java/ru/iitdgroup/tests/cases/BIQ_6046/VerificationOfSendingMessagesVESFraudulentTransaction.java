package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VerificationOfSendingMessagesVESFraudulentTransaction extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_04_InfectedDevice";
    private static final String REFERENCE_ITEM1 = "(System_parameters) Интеграционные параметры";
    private static final String LOGIN_HASH = "777";
    private static final String LOGIN = "korol777";
    private static final String SESSION_ID = "popopo777";
    private static final String UNIQUENAME = "SEND_FROM_VES";
    private static String TRANSACTION_ID1;
    private static String TRANSACTION_ID2;

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private String workflowRecordUniqueName = "";


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
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH);
                client.getData().getClientData().getClient()
                        .withFirstName("Зинаида")
                        .withLastName("Любимова")
                        .withMiddleName("Марковна")
                        .withLogin(LOGIN)
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "отправка сообщения ВЭС с Раббита",
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
            description = "Выключить: VES_FFEDBACK = 0",
            dependsOnMethods = "goToLoggedMessages"
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
            dependsOnMethods = "enableVES2"
    )

    public void step0() {
        Transaction transaction = getTransaction();
        String transaction_id = transaction.getData().getTransactionData().getTransactionId();
        TRANSACTION_ID1 = transaction_id;

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
        String transaction_id = transaction.getData().getTransactionData().getTransactionId();
        TRANSACTION_ID2 = transaction_id;
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
            description = "Перейти в сформированный алерт и проставить Статус = Обработано, Резолюция = Мошенничество и" +
                    "Перейти в транзакцию №1 и выполнить Action Send_from_VES",
            dependsOnMethods = "step0"
    )

    public void goToAletrt() {
        getIC()
                .locateAlerts().sleep(10)
                .openFirst()
                .action("Мошенничество")
                .goToTransactionPage()
                .action(workflowRecordUniqueName)
                .sleep(3);
    }

    @Test(
            description = "Проверить логированные сообщения на отправку сообщения ВЭС"
//            dependsOnMethods = "goToAletrt"
    )

    public void goToLoggedMessages() {
        getIC()
                .locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .filterDeactivation1()
                .filterDeactivation2()
                .setTableFilter("Смежная система", "Equals", "VES")
                .refreshTable().sleep(3)
        ;
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
