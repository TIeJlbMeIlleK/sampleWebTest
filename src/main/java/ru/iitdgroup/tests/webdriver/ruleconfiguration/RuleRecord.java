package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class RuleRecord extends AbstractEdit<RuleRecord> {
    private final RemoteWebDriver driver;

    @Override
    protected RuleRecord getSelf() {
        return this;
    }

    public RuleRecord(RemoteWebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    public RuleEdit edit(){
        driver.findElementByXPath("//a[@id='btnEdit']").click();
        return new RuleEdit( driver);
    }
}
