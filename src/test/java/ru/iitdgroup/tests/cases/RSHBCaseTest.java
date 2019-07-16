package ru.iitdgroup.tests.cases;

import org.openqa.selenium.Dimension;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Template;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.dbdriver.Database;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.IC;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public abstract class RSHBCaseTest {

    protected static final String NOT_TRIGGERED = "NOT_TRIGGERED";
    protected static final String TRIGGERED = "TRIGGERED";
    protected static final String FEW_DATA = "FEW_DATA";
    protected static final String REGULAR_TRANSACTION = "Правило не применяется для регулярных транзакций";
    protected static final String REGULAR_TRANSACTION_1 = "Правило не применяется для регулярных транзакций.";
    protected static final String RESULT_RULE_NOT_APPLY = "Правило не применилось";
    protected static final String RESULT_RULE_NOT_APPLY_BY_CONF = "Правило не применилось (проверка по настрокам правила)";
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
    protected static final String RESULT_GREY_IP = "IP адрес найден в Сером списке %s";
    protected static final String RESULT_NO_GREY_IP = "IP адрес не найден в Сером списке %s";
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
    protected static final String MISSING_DEVICE_1 = "В системе нет данных об устройстве клиента";
    protected static final String INTERNET_BANK_TRANSACTION = "Правило не применяется для платежей интернет банка";
    protected static final String NO_IMEI_EXR9 = "Нет индентификатора производителя (IMEI)";
    protected static final String NO_IMSI_EXR9 = "Нет индентификатора производителя (IMSI)";
    protected static final String NO_IFV_EXR9 = "Нет индентификатора производителя (IFV)";
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







    private DBOAntiFraudWS ws;
    private TestProperties props;
    private IC ic;
    private Database database;

    @BeforeClass
    public void setUpProperties() throws IOException {
        props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
    }

    @BeforeMethod
    public void before() {
        ws = new DBOAntiFraudWS(
                getProps().getWSUrl(),
                getProps().getWSUser(),
                getProps().getWSPassword());
    }

    @AfterClass
    public void tearDown() {
        if (ic != null) {
            getIC().close();
        }
    }

    protected DBOAntiFraudWS getWS() {
        return ws;
    }

    protected DBOAntiFraudWS send(Template template) {
        try {
            return getWS().send(template);
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

    protected TestProperties getProps() {
        return props;
    }

    protected IC getIC() {
        if (ic == null) {
            ic = new IC(getProps());
            ic.getDriver().manage().window().setSize(new Dimension(1000, 1000));
        }
        return ic;
    }

    protected Database getDatabase() {
        if (database == null) {
            database = new Database(getProps());
        }
        return database;
    }

    protected String[][] getResults(String ruleName) {
        try {
            return getDatabase()
                    .select()
                    .field("EXECUTION_TYPE")
                    .field("DESCRIPTION")
                    .from("INCIDENT_WRAP")
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
        String[][] dbResult = getResults(getRuleName());
        assertEquals(ruleResult, dbResult[0][0]);
        assertEquals(description, dbResult[0][1]);
    }

    protected Transaction getTransaction(String filePath) {
        try {
            //FIXME Добавить проверку на существование клиента в базе
            Transaction transaction = new Transaction(filePath);
            transaction.getData()
                    .getTransactionData()
                    .withTransactionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "")
                    .withSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "")
                    .withDocumentNumber(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
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

}
