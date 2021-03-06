package ru.iitdgroup.tests.cases;

import net.bytebuddy.utility.RandomString;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.cluster.ClusterGroup;
import org.junit.Assert;
import org.openqa.selenium.Dimension;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import ru.iitdgroup.tests.apidriver.*;
import ru.iitdgroup.tests.dbdriver.Database;
import ru.iitdgroup.tests.ignitedriver.SampleIgnite;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.IC;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;

public abstract class RSHBCaseTest {

    protected static final String NOT_TRIGGERED = "NOT_TRIGGERED";
    protected static final String TRIGGERED = "TRIGGERED";
    protected static final String FEW_DATA = "FEW_DATA";
    protected static final String EXCEPTION = "EXCEPTION";
    protected static final String REGULAR_TRANSACTION = "Правило не применяется для регулярных транзакций";
    protected static final String REGULAR_TRANSACTION_1 = "Правило не применяется для регулярных транзакций.";
    protected static final String RESULT_RULE_NOT_APPLY = "Правило не применилось";
    protected static final String RESULT_RULE_NOT_APPLY_EXR_07 = "Правило не применилось.";
    protected static final String RESULT_RULE_NOT_APPLY_BY_CONF = "Правило не применилось (проверка по настройкам правила)";
    protected static final String RESULT_RULE_NOT_APPLY_EMPTY = "В выборке только анализируемая транзакция";
    protected static final String RESULT_RULE_APPLY_BY_LENGTH = "Количество транзакций больше параметра Длина серии";
    protected static final String RESULT_RULE_APPLY_BY_SUM = "Общая сумма транзакций больше допустимой величины";
    protected static final String RESULT_RULE_CARD_IN_BLACK_LIST = "Карта получателя в чёрном списке";
    protected static final String NOT_EXIST_IN_BLACK_LIST = "Нет совпадений по параметрам со списками запрещенных";
    protected static final String RESULT_RULE_PAYEE_ACCOUNT_IS_ON_THE_BLACK_LIST = "Сводный Счет и Счет/карта получателя в чёрном списке";
    protected static final String RESULT_RULE_BIK_IN_BLACK_LIST = "BIK/ACC получателя в чёрном списке";
    protected static final String RESULT_RULE_INN_IN_BLACK_LIST = "INN получателя в чёрном списке";
    protected static final String RESULT_BLOCK_CONSOLIDATED_INN = "Сводный Счет и INN получателя в чёрном списке";
    protected static final String RESULT_BLOCK_CONSOLIDATED_NAME = "Сводный ФИО получателя в чёрном списке";
    protected static final String RESULT_BLOCK_MT_SYSTEM = "Получатель платежа в черном списке";
    protected static final String RESULT_SPEED_NORMAL = "Промежуток времени между транзакциями больше интервала";
    protected static final String RESULT_SPEED = "Промежуток времени между транзакциями меньше интервала";
    protected static final String RESULT_GREY_IP = "IP адрес найден в Сером списке";
    protected static final String RESULT_NO_GREY_IP = "IP адрес не найден в Сером списке";
    protected static final String RESULT_GREY_IMSI = "IMSI найден в сером списке";
    protected static final String RESULT_GREY_IMEI = "IMEI найден в сером списке";
    protected static final String RESULT_GREY_IFV = "IFV найден в сером списке";
    protected static final String RESULT_GREY_DFP = "DFP найден в сером списке";
    protected static final String RESULT_GREY_IMSI_AMD_IMEI = "IMSI найден в сером списке";
    protected static final String RESULT_FEW_DATA = "Недостаточно данных";
    protected static final String RESULT_DEVICE_NULL = "Нет устройств";
    protected static final String NO_TRASACTION_WITH_SAME_IMEI = "Нет транзакций с таким же IMEI, выполненных другим клиентом.";
    protected static final String NO_TRASACTION_WITH_SAME_IMSI = "Нет транзакций с таким же IMSI, выполненных другим клиентом.";
    protected static final String RESULT_HAS_TRANSACTIONS = "Найдена транзакция с этого устройства, сделанная другим клиентом, который не находится в списке доверенных.";
    protected static final String EXIST_TRUSTED_IMEI = "Существует доверенное устройство с таким IMEI.";
    protected static final String EXIST_TRUSTED_IMSI = "Существует доверенное устройство с таким IMSI.";
    protected static final String DISABLED_GIS_SETTING = "Правило не применяется, в системе выключена проверка GIS_SYSTEM_GIS";
    protected static final String REQUIRE_IP = "Нет данных об ip адресе";
    protected static final String NO_DEVICE = "Нет устройств";
    protected static final String REQUIRE_GEOLOCATION = "Недосточно данных: для полученного ip адреса 151.555.555.1 нет данных о геолокации";
    protected static final String REQUIRE_PREVIOUS_TRANSACTION = "Нет данных о предыдущей транзакции";
    protected static final String RESULT_HISPEED_150 = "Растояние/время между транзакциями превышает 150км/ч";
    protected static final String RESULT_HISPEED_400 = "Растояние/время между транзакциями превышает 400км/ч";
    protected static final String RESULT_HISPEED_800 = "Растояние/время между транзакциями превышает 800км/ч";
    protected static final String MISSING_DEVICE = "В системе нет данных об устройстве клиента.";
    protected static final String NO_IMSI = "Нет идентификатора абонента (IMSI).";
    protected static final String NO_IMEI = "Нет идентификатора оборудования (IMEI).";
    protected static final String NO_IFV = "Нет индентификатора производителя (IFV).";
    protected static final String DISABLED_INTEGR_VES = "Правило не применяется. В системе выключена проверка INTEGR_VES.";
    protected static final String DISABLED_INTEGR_VES_1 = "Правило не применяется, в системе выключена проверка INTEGR_VES";
    protected static final String DISABLED_IntegrVES1 = "Правило не применяется, в системе выключена проверка IntegrVES1";
    protected static final String BIG_TRANSFER = "Cумма транзакции больше значение параметра «Крупный перевод»";
    protected static final String MISSING_DEVICE_1 = "В системе нет данных об устройстве клиента";
    protected static final String INTERNET_BANK_TRANSACTION = "Правило не применяется для платежей интернет банка";
    protected static final String NO_IMEI_EXR9 = "Нет индентификатора производителя (IMEI)";
    protected static final String NO_IMSI_EXR9 = "Нет индентификатора производителя (IMSI)";
    protected static final String REGULAR_TRANSACTION_EXR9 = "Правило не применяется для регулярных платежей";
    protected static final String DEVICE_NOT_EXIST = "Отсутствуют доверенные устройства";
    protected static final String EXIST_TRUSTED_ANDROID_DEVICE = "Существует доверенное устройство с такими IMEI и IMSI";
    protected static final String EXIST_TRUSTED_IFV = "Существует доверенное устройство с таким IFV";
    protected static final String EXIST_TRUSTED_IMEI_2 = "Найдено доверенное устройство для клиента: совпадение по IMEI";
    protected static final String EXIST_TRUSTED_IMSI_2 = "Найдено доверенное устройство для клиента: совпадение по IMSI";
    protected static final String NEW_DEVICE = "У клиента новое устройство";
    protected static final String SUSPICIOUS_DEVICE = "Подозрительное устройство";
    protected static final String ANOTHER_TRANSACTION_TYPE = "Правило не применяется для транзакций такого типа";
    protected static final String RESULT_ALERT_FROM_VES = "В ответе от ВЭС присутствуют признаки заражения или удаленного управления устройтвом клиента";
    protected static final String YOUNG_QUARANTINE_LOCATION = "Местоположение недавно находится в карантине";
    protected static final String RESULT_ADD_QUARATINE_LOCATION = "Добавлено новое значение в справочник 'Карантин месторасположения'";
    protected static final String RESULT_RULE_APPLY_BY_SUM_GR_25 = "Значения пороговых величин превышены";
    protected static final String RESULT_RULE_NOT_APPLY_BY_CONF_GR_25 = "Правило не применилось (проверка по настройкам правила)";
    protected static final String RESULT_EMPTY_MAXAMOUNTLIST = "Пустой список PaymentMaxAmount для Клиента";
    protected static final String RESULT_STRING_WITH_PARAMETER = "Cумма транзакции=%s  максимальная транзакция=%s  конфигурация правила=%s";
    protected static final String RESULT_ANOMAL_TRANSFER = "Превышение порога отклонения от максимальной суммы транзакции Клиента";
    protected static final String GOOD_VES_CODE = "ВЭС не обнаружил опасных воздействий на устройство";
    protected static final String REQUIRE_VES_DATA = "Полученные от ВЭС коды ответов не добавлены в настройку правила";
    protected static final String NOT_CARD_OR_SERVICE = "Транзакция не на карту и не сервисный";
    protected static final String EMPTY_ADDITIONAL_FIELDS = "Пустой список дополнительных свойств";
    protected static final String IN_WHITE_LIST = "Получатель найден в списке разрешенных";
    protected static final String ADD_TO_QUARANTINE_LIST = "Получатель добавлен в список карантина";
    protected static final String PAYEE_NOT_IDENTIFIED = "Получатель не идентифицирован";
    protected static final String RESULT_EXIST_QUARANTINE_LOCATION = "Получатель уже находится в карантине";
    protected static final String YOUNG_QUARANTINE = "Получатель недавно находится в карантине";
    protected static final String EMPTY_WHITE_LIST = "Не найдено совпадений параметров со списком разрешенных";
    protected static final String RESULT_RULE_NOT_APPLY_BIG_AMOUNT = "Правило не применилось, крупный перевод";
    protected static final String EXIST_IN_WHITE_LIST = "В списке разрешенных найдены совпадающие параметры";
    protected static final String RESULT_RULE_NOT_APPLY_BY_PERIOD = "Правило не применилось, т.к. найдены другие транзакции за период";
    protected static final String EMPTY_VIP_LIST = "Не найдено совпадений параметров со списком VIP клиентов";
    protected static final String EXIST_IN_VIP_LIST = "В списке VIP клиентов найденны совпадающие параметры";
    protected static final String RESULT_BLOCK_PHONE = "Телефон получателя в чёрном списке";
    protected static final String REPLACE_SIM = "Произошла замена SIM";
    protected static final String REPLACE_IMEI = "Произошла замена IMEI";
    protected static final String RESULT_RULE_APPLY_BY_LENGHT = "Количество транзакций больше допустимой длины серии";
    protected static final String RESULT_BIG_TRANSFER = "Осуществление крупного перевода средств";
    protected static final String RESULT_GRAY_LIST = "Найдено совпадение с серым списком";
    protected static final String NO_MATCHES_FOUND = "Совпадений не найдено";
    protected static final String NO_DATA_TO_ANALYZE = "Отсутствуют данные для анализа";
    protected static final String RESULT_GRAY_BANK = "Подозрительный БИН банка";
    protected static final String RESULT_NOT_GREY_BIN = "Не подозрительный БИН банка";
    protected static final String MISSING_CARD_NUMBER = "Отсутствует номер карты получателя";
    protected static final String RESULT_RULE_NOT_APPLY_MAX_SUM = "Допустимая сумма перевода";
    protected static final String RESULT_RULE_NOT_APPLY_REGULAR = "Правило не применяется для периодических платежей";
    protected static final String FALSE_EX_IR1 = "Проверяемая пара канал ДБО - тип транзакции";
    protected static final String EX_IR1 = "Непроверяемая пара канал ДБО - тип транзакции";
    protected static final String RESULT_CHANGE_CONTACT = "Изменен контакт клиента для аутентификации";
    protected static final String RESULT_CHANGE_IMSI = "Изменен IMSI телефона для аутентификации";
    protected static final String RESULT_ATTENTION_CLIENT = "Клиент с пометкой Особое внимание";
    protected static final String EX_WR2 = "Транзакция в сторону государства";
    protected static final String NOT_EXIST_OLD_ACCOUNT = "Нет совпадений со списком старых клиентов";
    protected static final String EXIST_OLD_ACCOUNT = "Есть совпадение со списком старых клиентов";
    protected static final String RESULT_RESET_BALANCE = "Обнуление остатка";
    protected static final String NO_TRUSTED_IMSI = "Нет доверенного устройства с таким IMSI. Будут проверяться транзакции.";
    protected static final String NO_TRUSTED_IMEI = "Нет доверенного устройства с таким IMEI. Будут проверяться транзакции.";
    protected static final String RESULT_GRAYBIK_BANK = "Банк получателя находится в сером списке";
    protected static final String RESULT_GRAY_SYSTEM = "Система денежных переводов получателя находится в сером списке";
    protected static final String RESULT_GRAY_BENEFICIAR_INN = "Получатель находится в сером списке по ИНН";
    protected static final String RESULT_GRAY_BENEFICIAR_BIC_ACC = "Получатель находится в сером списке по БИК + Счет";
    protected static final String EXISTS_MATCHES = "Правило применилось, найдены совпадающие параметры";
    protected static final String NO_MATCHES = "Нет совпадений";
    protected static final String PHONE_CONDITIONS_NOT_MET = "Для типа «Перевод по номеру телефона» условия правила не выполнены";
    protected static final String TRIGGERED_TRUE = "Найдена подтвержденная транзакция с совпадающими реквизитами";
    protected static final String ANOTHER_TRANSACTION = "Непроверяемый тип транзакции";
    protected static final String RULE_CONDITIONS_NOT_MET = "Условия правила не выполнены";
    protected static final String SCENARIO_BLOCK_TRUE = "Выражение блока сценариев ИСТИННО!";
    protected static final String MAX_PERIOD_BETWEEN_PASSWORD_RECOVERY_FIRST_TRANSACTION = "Превышен период между моментом восстановления доступа к ДБО и первой транзакцией серии";
    protected static final String FALSE_EX_IR2 = "Клиент не иcключён из проверки";
    protected static final String EX_IR2 = "Клиент исключен из проверки";
    protected static final String DISABLED_INTEGR_VES_NEW = "Правило не применяется, в системе выключена проверка IntegrVES2";
    protected static final String RESULT_FEW_DATA_NEW = "Не достаточно данных";
    protected static final String RESULT_ALERTS = "В ответе ВЕС присутствует признак 'Анормальная смена устройства' или 'Новое устройство у клиента'";
    protected static final String RESULT_HAS_TRANSACTIONS_NEW = "Найдена транзакция с этого устройства, сделанная другим клиентом. " +
            "Устройство не находится в списке доверенных для текущего клиента.";
    protected static final String RESULT_EXIST_QUARANTINE_LOCATION_FOR_IP = "Местоположение уже находится в карантине";
    protected static final String RESULT_RULE_NOT_APPLY_NO_TYPE_TRANZACTION = "Правило не применилось (Метод не возвращает суммы для данного типа транзакции): ";
    protected static final String NOT_APPLY_SUCH_TRANSACTION_TYPE = "Правило не применяется для данного типа транзакции";
    protected static final String WRONG_TRANSACTION_TYPE = "Не тот тип транзакции";
    protected static final String EXIST_TRUSTED_DEVICE_MSG = "Устройство клиента найдено в списке ранее использовавшихся";
    protected static final String RESULT_RULE_APPLY_ADAK = "Попытка подбора ответа на АДАК";
    protected static final String ANOMAL_GEO_CHANGE = "Аномальная смена геопозиции Клиентом";
    protected static final String RULE_TRIGGERED = "Сценарий сработал";
    protected static final String RESULT_RULE_APPLY = "Найдены необработаные события для этого клиента";
    protected static final String RESULT_YOUNG_MAN = "Заявка на выпуск карты(цифровая , 15 лет)";
    protected static final String RESULT_OLD_MAN = "Тип транзакции «Заявка на выпуск карты» (тип карты «виртуальная», возраст клиента больше 18)";
    protected static final String RESULT_TRIGGERED = "Количество однотипных транзакций больше допустимой длины серии";

    private static final String DBO_NAME_TABLE_INCIDENTS   = "INCIDENT_WRAP";
    private static final String DBO_NAME_TABLE_TRANSACTION = "PAYMENT_TRANSACTION";

    private DBOAntiFraudWS ws;
    private ESPP2AntiFraudWS esppWs;
    private TestProperties props;
    private IC ic;
    private Rabbit rabbit;
    private Database database;
    private Ignite ignite;

    @BeforeClass
    public void setUpProperties() throws IOException {
        props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
        ignite = SampleIgnite.runLocalignite(true);
    }

    @BeforeMethod
    public void before() {
        ws = new DBOAntiFraudWS(
                getProps().getWSUrl(),
                getProps().getWSUser(),
                getProps().getWSPassword());
        esppWs = new ESPP2AntiFraudWS(
                getProps().getEsppWSUrl(),
                getProps().getWSUser(),
                getProps().getWSPassword());
    }

    @AfterClass
    public void tearDown() {
        if (ic != null) {
            getIC().close();
        }
        ignite.close();
    }

    protected IgniteMessaging getMsg() {
        ClusterGroup clients = ignite
                .cluster()
                .forClients()
                .forAttribute("ROLE", "clientNode");
        return ignite.message(clients);
    }

    protected DBOAntiFraudWS getWS() {
        return ws;
    }

    protected ESPP2AntiFraudWS getWsEspp() {
        return esppWs;
    }

    protected DBOAntiFraudWS send(Template template) {
        try {
            return getWS().send(template);
        } catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }

    protected ESPP2AntiFraudWS sendEspp(Template template) {
        try {
            return getWsEspp().send(template);
        } catch (SOAPException e) {
            throw new IllegalStateException(e);
        }
    }


    protected DBOAntiFraudWS sendAndAssert(Template template) {
        DBOAntiFraudWS result = send(template);
        assertTrue(
                String.format("Ошибка на стороне AntiFraudWS: %s", result.getResponse().getErrorMessage()),
                result.isSuccessResponse());
        return result;
    }

    protected ESPP2AntiFraudWS sendEsppAndAssert(Template template) {
        ESPP2AntiFraudWS result = sendEspp(template);
        assertTrue(
                String.format("Ошибка на стороне AntiFraudWS: %s", result.getResponse().getErrorMessage()),
                result.isSuccessResponse());
        return result;
    }

    protected String[] getScenarioBlock() {
        try {
            String[][] id = getDatabase()
                    .select()
                    .field("id")
                    .from("SCENARIO_BLOCK")
                    .sort("id", false)
                    .limit(2)
                    .get();
            return new String[]{id[0][0], id[1][0]};

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Возвращает поцизию (широта, долгота) клиента
     *
     * @param DBO_Id dbo-id клиента
     * @return массив их двух чисел (latitude, longitude)
     */
    protected String[] getClientsPosition(String DBO_Id) {
        try {
            Thread.sleep(1000);
            String[][] id = getDatabase()
                    .select()
                    .field("id")
                    .from("Client")
                    .with("DBO_ID", "=", "'" + DBO_Id + "'")
                    .sort("id", false)
                    .limit(1)
                    .get();
            String[][] result = getDatabase()
                    .select()
                    .field("LATITUDE")
                    .field("LONGITUDE")
                    .from("QUARANTINE_LOCATION")
                    .with("CLIENT_FK", "=", id[0][0])
                    .sort("id", false)
                    .limit(1)
                    .get();
            String latitude = result[0][0].replace('.', ',');
            String longitude = result[0][1].replace('.', ',');
            return new String[]{latitude, longitude};

        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected TestProperties getProps() {
        return props;
    }

    protected IC getIC() {
        if (ic == null) {
            ic = new IC(getProps());
            ic.getDriver().manage().window().setSize(new Dimension(2000, 10000));
        }
        return ic;
    }

    protected Rabbit getRabbit() {
        if (rabbit == null) {
            rabbit = new Rabbit(getProps());
            rabbit.getDriver().manage().window().setSize(new Dimension(2000, 3000));
        }
        return rabbit;
    }

    protected Database getDatabase() {
        return new Database(getProps());
    }

    protected String getNameTableIncidents() {
        return DBO_NAME_TABLE_INCIDENTS;
    }

    protected String[][] getIncidentWrapByRule(String ruleName) {
        try {
             return getDatabase()
                    .select()
                    .field("EXECUTION_TYPE")
                    .field("DESCRIPTION")
                    .from(getNameTableIncidents())
                    .with("RULE_TITLE", "=", "'" + ruleName + "'")
                    .sort("id", false)
                    .limit(1)
                    .get();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected void assertLastTransactionRuleApply(String ruleResult, String description) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        String[][] dbResult = getIncidentWrapByRule(getRuleName());

        assertEquals(ruleResult, dbResult[0][0]);
        assertEquals(description, dbResult[0][1]);
    }

    protected void assertRuleResultForTheLastTransaction(String ruleName, String ruleResult, String description) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        String[][] dbResult = getIncidentWrapByRule(ruleName);
        assertEquals(ruleResult, dbResult[0][0]);
        assertEquals(description, dbResult[0][1]);
    }

    /**
     * Возвращает информацию о последнем отправленном СМС
     *
     * @return массив:
     * [id,
     * JMS_CORRELATION_ID,
     * JMS_MESSAGE_ID,
     * MESSAGE,
     * MESSAGE_DATE,
     * MSISDN,
     * SECRET_KEY_DATE,
     * SMS_SECRET_KEY_FK]
     */
    protected String[] getLastSentSMSInformation() {
        try {
            String[][] results = getDatabase()
                    .select()
                    .field("id")
                    .field("JMS_CORRELATION_ID")
                    .field("JMS_MESSAGE_ID")
                    .field("MESSAGE")
                    .field("MESSAGE_DATE")
                    .field("MSISDN")
                    .field("SECRET_KEY_DATE")
                    .field("SMS_SECRET_KEY_FK")
                    .from("SMS_MESSAGE")
                    .sort("MESSAGE_DATE", false)
                    .limit(1)
                    .get();
            return results[0];
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected String getNameTableTransactions() {
        return DBO_NAME_TABLE_TRANSACTION;
    }

    protected void assertTransactionAdditionalFieldApply(String transactionID, String fieldId, String fieldName, String fieldValue) {
        try {
            Thread.sleep(1000);
            String[][] id = getDatabase()
                    .select()
                    .field("id")
                    .from(getNameTableTransactions())
                    .with("TRANSACTION_ID", "=", "'" + transactionID + "'")
                    .sort("timestamp", false)
                    .limit(1)
                    .get();
            String[][] result = getDatabase()
                    .select()
                    .field("ADDITIONAL_FIELD_ID")
                    .field("ADDITIONAL_FIELD_NAME")
                    .field("ADDITIONAL_FIELD_VALUE")
                    .from("ADDITIONAL_FIELD_TYPE")
                    .with("transaction_id", "=", id[0][0])
                    .sort("timestamp", false)
                    .limit(1)
                    .get();
            assertEquals(fieldId, result[0][0]);
            assertEquals(fieldName, result[0][1]);
            assertEquals(fieldValue, result[0][2]);
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Возвращает значение поля fieldName из таблицы tableName с последним (максимальным) id
     *
     * @param tableName название таблицы
     * @param fieldName название поля
     * @return пара (value,id), где value - значение поля fieldName, id - последний id в таблице, которому соответствует value
     */
    protected String[] getFieldWithLastId(String tableName, String fieldName) {
        try {
            String[][] result = getDatabase()
                    .select()
                    .field(fieldName)
                    .field("id")
                    .from(tableName)
                    .sort("id", false)
                    .limit(1)
                    .get();
            return result[0];
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected void assertClientEmailApply(String dboID, String email) {
        try {
            Thread.sleep(1000);

            String[][] result = getDatabase()
                    .select()
                    .field("id")
                    .field("AUTH_CHANGE_TIMESTAMP")
                    .field("NOTIFICATION_EMAIL")
                    .field("DATE_REGISTRATION")
                    .from("Client")
                    .with("DBO_ID", "=", dboID)
                    .sort("AUTH_CHANGE_TIMESTAMP", false)
                    .limit(1)
                    .get();
            assertNull(result[0][1]);//проверяет, что строка пустая
            assertEquals(email, result[0][2]);//проверяет на наличе email в строке
            assertNotNull(result[0][3]);//проверяет на наличе регистрационной даты в строке
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected void assertClientEmailChanged(String dboID, String email) {
        try {
            Thread.sleep(1000);

            String[][] result = getDatabase()
                    .select()
                    .field("id")
                    .field("AUTH_CHANGE_TIMESTAMP")
                    .field("NOTIFICATION_EMAIL")
                    .field("DATE_REGISTRATION")
                    .from("Client")
                    .with("DBO_ID", "=", dboID)
                    .sort("AUTH_CHANGE_TIMESTAMP", false)
                    .limit(1)
                    .get();
            assertNotNull(result[0][1]);//строка не пустая, есть запись даты изменения email
            assertEquals(email, result[0][2]);//есть запись еmail
            assertNotNull(result[0][3]);//есть дата регистрации клиента
            Assert.assertNotEquals(result[0][1], result[0][3]);//даты регистрации и изменения email не равны
        } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    protected void assertPaymentMaxAmount(String clientId, String txType, BigDecimal amount) {
        try {
            String[][] result = getDatabase()
                    .select()
                    .field("CLIENT_DBO_ID")
                    .field("MAX_AMOUNT")
                    .from("PAYMENT_MAX_AMOUNT")
                    .with("object_type", "=", "'" + txType + "'")
                    .with("CLIENT_DBO_ID", "=", clientId)
                    .with("MAX_AMOUNT", "=", amount.toString())
                    .sort("CLIENT_DBO_ID", true)
                    .setFormula("AND")
                    .get();
            assertEquals(1, result.length);
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    protected void assertPaymentMaxAmountVersionDoc(String clientId, String documentVersion, String transactionId, String txType, BigDecimal amount) {
        try {
            String[][] result = getDatabase()
                    .select()
                    .field("CLIENT_DBO_ID")
                    .field("MAX_AMOUNT")
                    .from("PAYMENT_MAX_AMOUNT")
                    .with("object_type", "=", "'" + txType + "'")
                    .with("DOCUMENT_VERSION", "=", documentVersion)
                    .with("TRANSACTION_ID", "=", transactionId)
                    .with("CLIENT_DBO_ID", "=", clientId)
                    .with("MAX_AMOUNT", "=", amount.toString())
                    .sort("CLIENT_DBO_ID", true)
                    .setFormula("AND")
                    .get();
            assertEquals(1, result.length);
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    protected void assertTableField(String fieldName, String expectedValue) {
        assertEquals(
                expectedValue,
                getIC().getDriver()
                        .findElementByXPath(String.format("//*[text()='%s']/../following-sibling::td", fieldName))
                        .getText());
    }

    protected void assertTableFieldInReportsTransaction(String fieldName, String expectedValue) {
        assertEquals(
                expectedValue,
                getIC().getDriver()
                        .findElementByXPath(String.format("//span[text()='%s']", fieldName))
                        .getText());
    }

    protected String copyThisLine(String fieldName) {
        String result = String.format("//span[text()='%s']/../following::td", fieldName);
        return getIC().getDriver().findElementByXPath(result).getText();
    }

    protected Transaction getTransaction(String filePath) {
        try {
            //FIXME Добавить проверку на существование клиента в базе
            Transaction transaction = new Transaction(filePath);
            transaction.getData()
                    .getTransactionData()
                    .withTransactionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "")
                    .withSessionId(new RandomString(40).nextString())
                    .withDocumentNumber(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
            return transaction;
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected TransactionEspp getTransactionESPP(String filePath) {
        try {
            TransactionEspp transaction = new TransactionEspp(filePath);
            transaction.getData()
                    .withTransactionId((ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7))
                    .withDocumentNumber((ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5));
            return transaction;
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Authentication getAuthentication(String filePath) {
        try {
            Authentication authentication = new Authentication(filePath);
            authentication.getData()
                    .getClientAuthentication()
                    .withSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
            return authentication;
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract String getRuleName();

    protected Ignite getIgnite() {
        return ignite;
    }

}
