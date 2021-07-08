package ru.iitdgroup.tests.cases.BIQ_8994;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_02_BudgetTransfer_end_manyPersonalException extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_02_BudgetTransfer";
    private static final String ruleName = "WR_02_BudgetTransfer";
    private static final String RULE_NAME_ExR_01 = "R01_ExR_01_AuthenticationContactChanged";
    private static final String RULE_NAME_ExR_03 = "R01_ExR_03_UseNewDevice";
    private static final String RULE_NAME_ExR_05 = "R01_ExR_05_GrayIP";
    private static final String RULE_NAME_GR_02 = "R01_GR_02_ResetBalance";
    private static final String RULE_NAME_GR_26 = "R01_GR_26_DocumentHashInGrayList";
    private static final String TABLE_HASH_DOCUMENT_CLIENT = "(Rule_tables) Подозрительные документы клиентов";
    private static final String TABLE_SUSPECT_IP = "(Rule_tables) Подозрительные IP адреса";
    private static final String TABLE_TRUSTED_GEOPOSITION = "(Rule_tables) Типичное расположение";
    private static final String SUSPECT_IP = "77.57.50.211";
    private static final String SESSION_ID = new RandomString(30).nextString();
    private static final String SESSION_ID1 = new RandomString(30).nextString();
    private final String[][] names = {{"Георгий", "Жуков", "Федорович"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final GregorianCalendar time3 = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+03:00");

    @Test(
            description = "6. Деактивировать все правила." +
                    "7. Включить правила: WR_02_BudgetTransfer,  ExR_01_AuthenticationContactChanged, " +
                    "ExR_03_UseNewDevice: (код ВЭС: 305),  ExR_05_GrayIP,  GR_52_RiskyTSP, " +
                    "GR_01_AnomalTransfer, GR_26_DocumentHashInGrayList" +
                    "8. Добавить в Белое правило WR_02_BudgetTransfer правила Исключения:" +
                    "- ExR_01_AuthenticationContactChanged," +
                    "- ExR_05_GrayIP," +
                    "- ExR_03_UseNewDevice," +
                    "- GR_02_ResetBalance," +
                    "- GR_26_DocumentHashInGrayList."
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_ExR_01)
                .selectRule(RULE_NAME_ExR_05)
                .selectRule(RULE_NAME_GR_26)
                .activate();
        getIC().locateRules()
                .editRule(RULE_NAME_ExR_03)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Коды ответов ВЭС")
                .attachAddingValue("Коды ответов ВЭС", "Идентификатор кода", "Equals", "26");
        getIC().locateRules()
                .editRule(RULE_NAME_GR_02)
                .fillCheckBox("Active:", true)
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .attachPersonalExceptions("ExR_03_UseNewDevice")
                .attachPersonalExceptions("ExR_05_GrayIP")
                .attachPersonalExceptions("GR_02_ResetBalance")
                .attachPersonalExceptions("GR_01_AnomalTransfer")
                .attachPersonalExceptions("GR_26_DocumentHashInGrayList")
                .attachPersonalExceptions("ExR_01_AuthenticationContactChanged")
                .sleep(10);
    }

    @Test(
            description = "Создаем клиента" +
                    "9. Hash действующего документа (из карточки клиента): от клиента №2 внесен в справочник " +
                    "(Rule_tables) Подозрительные документы клиентов" +
                    "10. У клиента №2 не было смены учетных данных." +
                    "11. В справочник Подозрительный IP внести IP адрес, с указанного IP отправлять транзакцию",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 8);
                String numberPassword = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
                String organization = "МВД " + new RandomString(10).nextString();
                Client client = new Client("testCases/Templates/client.xml");
                time2.add(Calendar.YEAR, -12);
                time3.add(Calendar.YEAR, -55);

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withBirthDate(new XMLGregorianCalendarImpl(time3))
                        .withPasswordRecoveryDateTime(time2)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                client.getData()// для получения уникального Hash документа, нужно внести изменения у клиента в этом блоке
                        .getClientData()
                        .getClientDocument()
                        .get(0)
                        .withDocType("21")
                        .withSeries("46 28")
                        .withNumber(numberPassword)
                        .withIssueDate(new XMLGregorianCalendarImpl(time2))
                        .withOrganization(organization);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        String hashDocumentClient;
        try {
            String[][] hash = getDatabase()//сохраняем в переменную Hash действующего документа из карточки клиента
                    .select()
                    .field("ACTIVE_DOCUMENT_HASH")
                    .from("Client")
                    .sort("id", false)
                    .limit(1)
                    .get();
            hashDocumentClient = hash[0][0];
            System.out.println(hashDocumentClient);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        getIC().locateTable(TABLE_HASH_DOCUMENT_CLIENT)
                .deleteAll()
                .addRecord()
                .fillInputText("Hash документа:", hashDocumentClient)
                .select("Причина занесения:", "Внешняя система")
                .save();
        getIC().locateTable(TABLE_SUSPECT_IP)
                .deleteAll()
                .addRecord()
                .fillInputText("IP устройства:", SUSPECT_IP)
                .save();
        getIC().locateTable(TABLE_TRUSTED_GEOPOSITION)
                .deleteAll();
    }

    @Test(
            description = "1. Отправить сообщение ВЭС:" +
                    "-- от клиента №1 с кодом 26 \"Обращение к опасному домену\" R01_ExR_03_UseNewDevice." +
                    "4. Отправить транзакцию №3 В сторону Государства от клиента №1:" +
                    "- с подозрительным IP из справочника," +
                    "- SessionId = session_id из ВЭС (должен быть уникальным!)" +
                    "- с нулевым остатком на счете InitialSourceAmount = 0, с суммой - 10 000; ",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time1.add(Calendar.MINUTE, -300);
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "26");
            json.put("type_title", "Обращение к опасному домену");
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("time", format.format(time1.getTime()));
            json.put("session_id", SESSION_ID);
            json.put("device_hash", SESSION_ID);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        time.add(Calendar.MINUTE, -25);
        Transaction transaction = getTransactionBudgetTransfer();
        sendAndAssert(transaction);
        String result[][] = getIncidentWrapByRule(RULE_NAME);
        String descriptionRule = result[0][1];
        System.out.println(descriptionRule);

        assertLastTransactionRuleApply(NOT_TRIGGERED, descriptionRule);//нет возможности написать точное описание отработки правила
        //т.к. неизвестна очередность сработки Персонального Исключения Белого правила, какое сработает первым
        //описание включает в себя название первого сработавшего правила.
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_05, TRIGGERED, "IP адрес найден в Сером списке");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_03, TRIGGERED, "Обращение к опасному домену");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_02, TRIGGERED, "Обнуление остатка");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_26, TRIGGERED, "Найдено совпадение с серым списком");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_01, NOT_TRIGGERED, "Правило не применилось");
    }

    @Test(
            description = "5. Отправить от клиента №2 новое сообщение ВЭС с другим кодом (не совпадающий с правилом) и  другим SessionId." +
                    "Удалить клиента №1 Hash действующего документа из справочника (Rule_tables) Подозрительные документы клиентов," +
                    "деактивировать правило GR_01_AnomalTransfer и " +
                    "Отправить транзакцию №4 в Сторону Государства от клиента №1 спустя 30 минут:" +
                    "- не с подозрительными IP, с новым session_id из ВЭС, InitialSourceAmount = 100 000, с суммой - 1500.",
            dependsOnMethods = "step1"
    )
    public void step2() {
        getIC().locateTable(TABLE_HASH_DOCUMENT_CLIENT)
                .deleteAll();
        getIC().locateTable(TABLE_SUSPECT_IP)
                .deleteAll();

        time1.add(Calendar.MINUTE, -200);
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("customer_id", clientIds.get(0));
            json.put("type_id", "11");
            json.put("type_title", "Смена геолокации устройства");
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("time", format.format(time1.getTime()));
            json.put("session_id", SESSION_ID1);
            json.put("device_hash", SESSION_ID1);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();
            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        time.add(Calendar.MINUTE, 20);
        Transaction transaction = getTransactionBudgetTransfer();
        transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(100000.00))
                .withSessionId(SESSION_ID1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Транзакция в сторону государства");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_05, NOT_TRIGGERED, "IP адрес не найден в Сером списке");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_03, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_02, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_26, NOT_TRIGGERED, "Совпадений не найдено");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_01, NOT_TRIGGERED, "Правило не применилось");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(0))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withSessionId(SESSION_ID);
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataType
                .getBudgetTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1500.00));
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIpAddress(SUSPECT_IP);
        return transaction;
    }
}
