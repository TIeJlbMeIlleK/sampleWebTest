package ru.iitdgroup.tests.cases;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Template;
import ru.iitdgroup.tests.dbdriver.Database;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.IC;

import javax.xml.soap.SOAPException;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public abstract class RSHBCaseTest {

    protected static final String NOT_TRIGGERED = "NOT_TRIGGERED";
    protected static final String TRIGGERED = "TRIGGERED";
    protected static final String REGULAR_TRANSACTION = "Правило не применяется для регулярных транзакций";
    protected static final String RESULT_RULE_NOT_APPLY = "Правило не применилось";
    protected static final String RESULT_RULE_NOT_APPLY_BY_CONF = "Правило не применилось (проверка по настрокам правила)";
    protected static final String RESULT_RULE_NOT_APPLY_EMPTY = "В выборке только анализируемая транзакция";
    protected static final String RESULT_RULE_APPLY_BY_LENGTH = "Количество транзакций больше параметра Длина серии";
    protected static final String RESULT_RULE_APPLY_BY_SUM = "Общая сумма транзакций больше допустимой величины";

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

    protected DBOAntiFraudWS sendSuccess(Template template) {
        DBOAntiFraudWS result = send(template);
        if (!result.isSuccessResponse()) {
            throw new IllegalStateException("response is not success");
        }
        return result;
    }

    protected TestProperties getProps() {
        return props;
    }

    protected IC getIC() {
        if (ic == null) {
            ic = new IC(getProps());
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

    protected void assertLastTransactionRuleApply(boolean triggered, String description) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        String[][] dbResult = getResults(getRuleName());
        assertEquals(triggered ? TRIGGERED : NOT_TRIGGERED, dbResult[0][0]);
        assertEquals(description, dbResult[0][1]);
    }

    protected abstract String getRuleName();

}
