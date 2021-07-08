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

public class WR_03_end_manyPersonalException extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private static final String RULE_NAME_ExR_04 = "R01_ExR_04_InfectedDevice";
    private static final String RULE_NAME_ExR_06 = "R01_ExR_06_GrayDevice";
    private static final String RULE_NAME_ExR_08 = "R01_ExR_08_AttentionClient";
    private static final String RULE_NAME_GR_02_ResetBalance = "R01_GR_02_ResetBalance";
    private static final String RULE_NAME_GR_15_NonTypicalGeoPosition = "R01_GR_15_NonTypicalGeoPosition";
    private static final String TABLE_CLIENT_SPECIAL_ATTENTION = "(Rule_tables) Список клиентов с пометкой особое внимание";
    private static final String TABLE_TRUSTED_RECIPIENTS = "(Rule_tables) Доверенные получатели";
    private static final String TABLE_SUSPECT_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String TABLE_TRUSTED_GEOPOSITION = "(Rule_tables) Типичное расположение";
    private static final String TABLE_QUARANTIN_POSITION = "(Rule_tables) Карантин месторасположения";
    private static final String SUSPECT_IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String SUSPECT_IMEI1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String SUSPECT_IP = "10.28.45.183";
    private static final String CARD = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String CARD_TRUSTED = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String ACCOUNT = "105634" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String INN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String TRUSTED_PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String SERVICE = "МТС мобильная связь";
    private static final String PROVIDER = "МТС";
    private static final String SESSION_ID = new RandomString(30).nextString();
    private static final String SESSION_ID1 = new RandomString(30).nextString();
    private final String[][] names = {{"Анастасия", "Смирнова", "Витальевна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+03:00");

    @Test(
            description = "1. Включить правила: WR_03_EntrustedTransfer (Параметры: крупный перевод: 5000, период: 10, обнуление: 0.95), " +
                    "ExR_04_InfectedDevice: (код ВЭС: 91), ExR_06_GrayDevice, ExR_08_AttentionClient,  " +
                    "GR_02_ResetBalance (обнуление: 0.95), GR_15_NonTypicalGeoPosition." +
                    "2. Добавить в Белое правило WR_03_EntrustedTransfer правила Исключения:" +
                    "- GR_15_NonTypicalGeoPosition," +
                    "- GR_02_ResetBalance," +
                    "- ExR_06_GrayDevice," +
                    "- ExR_08_AttentionClient," +
                    "- ExR_04_InfectedDevice."
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_ExR_06)
                .selectRule(RULE_NAME_ExR_08)
                .selectRule(RULE_NAME_GR_15_NonTypicalGeoPosition)
                .activate()
                .editRule(RULE_NAME_ExR_04)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Коды ответов ВЭС")
                .attachAddingValue("Коды ответов ВЭС", "Идентификатор кода", "Equals", "26")
                .backToAllTheRules()
                .editRule(RULE_NAME_GR_02_ResetBalance)
                .fillCheckBox("Active:", true)
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .backToAllTheRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .attachPersonalExceptions("ExR_04_InfectedDevice")
                .attachPersonalExceptions("ExR_06_GrayDevice")
                .attachPersonalExceptions("ExR_08_AttentionClient")
                .attachPersonalExceptions("GR_02_ResetBalance")
                .attachPersonalExceptions("GR_15_NonTypicalGeoPosition")
                .sleep(10);
    }

    @Test(
            description = "Создаем клиента" +
                    "4. От Клиента №1 в справочник \"Доверенные получатели\" внесен доверенный получатель",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
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

        getIC().locateTable(TABLE_TRUSTED_RECIPIENTS)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", "Сергей")
                .fillInputText("Номер карты получателя:", CARD_TRUSTED)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT)
                .fillInputText("БИК банка получателя:", BIK)
                .fillInputText("ИНН получателя:", INN)
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", TRUSTED_PHONE)
                .fillInputText("Наименование сервиса:", SERVICE)
                .fillInputText("Наименование провайдера сервис услуги:", PROVIDER)
                .save();
        getIC().locateTable(TABLE_CLIENT_SPECIAL_ATTENTION)
                .deleteAll()
                .addRecord()
                .fillCheckBox("Признак «Особое внимание»:", true)
                .fillUser("Клиент:", clientIds.get(0))
                .save();
        getIC().locateTable(TABLE_SUSPECT_IMEI)
                .deleteAll()
                .addRecord()
                .fillInputText("imei:", SUSPECT_IMEI)
                .save();
        getIC().locateTable(TABLE_TRUSTED_GEOPOSITION)
                .deleteAll();
    }

    @Test(
            description = "1. Отправить сообщение ВЭС:" +
                    "-- от клиента №1 с кодом 26 «Обращение к опасному домену» " +
                    "R01_ExR_04_InfectedDevice (код соответсвует событию)" +
                    "2. Отправить транзакцию №1 Оплата услуг от клиента №1 на доверенное лицо из справочника (тел):" +
                    "- с нулевым остатком на счете InitialSourceAmount = 0" +
                    "- с подозрительным IMEI со справочника" +
                    "- IpAddress=10.28.45.183, (долгота: -97,82 и широта: 37,75)" +
                    "- SessionId = session_id из ВЭС (должен быть уникальным!)",
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

        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransferServicePayment();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData();
        transactionDataType
                .withSessionId(SESSION_ID);
        sendAndAssert(transaction);

        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сработало персональное исключение 'ExR_08_AttentionClient' белого правила");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_04, TRIGGERED, "Обращение к опасному домену");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_06, TRIGGERED, "IMEI найден в сером списке");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_08, TRIGGERED, "Клиент с пометкой Особое внимание");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_02_ResetBalance, TRIGGERED, "Обнуление остатка");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_15_NonTypicalGeoPosition, TRIGGERED, "Добавлено новое значение в справочник 'Карантин месторасположения'");
    }

    @Test(
            description = "Перенести из карантина местоположения в типичное расположение долготу и " +
                    "широту по клиенту №1, удалить Клиента №1 из \"Карантина месторасположения\" и  " +
                    "из \"Списка клиентов с пометкой особое внимание\", Отправить новое сообщение ВЭС " +
                    "с другим кодом (не совпадающий с правилом) и  другим SessionId  и Отправить транзакцию  " +
                    "№2 Оплата услуг от клиента №1 на доверенное лицо из справочника (тел):" +
                    "- с остатком InitialSourceAmount = 10 000" +
                    "- не с подозрительным IMEI" +
                    "- с новым session_id из нового сообщения  ВЭС.",
            dependsOnMethods = "step1"
    )
    public void step2() {
        getIC().locateTable(TABLE_QUARANTIN_POSITION)
                .deleteAll();
        getIC().locateTable(TABLE_TRUSTED_GEOPOSITION)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Долгота:", "-97,82")
                .fillInputText("Широта:", "37,75")
                .save();
        getIC().locateTable(TABLE_CLIENT_SPECIAL_ATTENTION)
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

        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransferServicePayment();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withSessionId(SESSION_ID1)
                .withInitialSourceAmount(BigDecimal.valueOf(100000.00));
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIMEI(SUSPECT_IMEI1)
                .withIpAddress(SUSPECT_IP);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "В списке разрешенных найдены совпадающие параметры");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_04, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_06, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_ExR_08, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_02_ResetBalance, NOT_TRIGGERED, "Правило не применилось");
        assertRuleResultForTheLastTransaction(RULE_NAME_GR_15_NonTypicalGeoPosition, NOT_TRIGGERED, "Найдено значение в справочнике 'Типичное расположение для клиента'");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withSessionId(SESSION_ID)
                .withInitialSourceAmount(BigDecimal.valueOf(0))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0)).withLoginHash(clientIds.get(0));
        transactionDataType
                .getServicePayment()
                .withProviderName(PROVIDER)
                .withServiceName(SERVICE)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1500.00))
                .withSourceCardNumber(CARD)
                .getAdditionalField()
                .get(0)
                .withId("ACCOUNT")
                .withName("По номеру телефона")
                .withValue(TRUSTED_PHONE);
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIMEI(SUSPECT_IMEI)
                .withIpAddress(SUSPECT_IP);
        return transaction;
    }
}
