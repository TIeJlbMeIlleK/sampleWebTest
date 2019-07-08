package ru.iitdgroup.tests.cases;

import org.openqa.selenium.Dimension;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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
    protected static final String REGULAR_TRANSACTION = "Правило не применяется для регулярных транзакций";
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
    protected static final String RESULT_GREY_IP = "IP адрес найден в Сером списке 192.168.1.2";
    protected static final String RESULT_NO_GREY_IP = "IP адрес не найден в Сером списке";
    protected static final String RESULT_GREY_IMSI = "IMSI найден в сером списке";
    protected static final String RESULT_GREY_IMEI = "IMEI найден в сером списке";
    protected static final String RESULT_GREY_IFV = "IFV найден в сером списке";
    protected static final String RESULT_GREY_DFP = "DFP найден в сером списке";
    protected static final String RESULT_GREY_IMSI_AMD_IMEI = "IMSI найден в сером списке";






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

    protected abstract String getRuleName();

}
