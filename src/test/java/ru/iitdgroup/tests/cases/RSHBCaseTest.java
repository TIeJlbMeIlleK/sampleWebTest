package ru.iitdgroup.tests.cases;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Template;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.IC;

import javax.xml.soap.SOAPException;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class RSHBCaseTest {

    private DBOAntiFraudWS ws;
    private TestProperties props;
    private IC ic;

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

    protected TestProperties getProps() {
        return props;
    }

    protected IC getIC() {
        if (ic == null) {
            ic = new IC(getProps());
        }
        return ic;
    }

}
