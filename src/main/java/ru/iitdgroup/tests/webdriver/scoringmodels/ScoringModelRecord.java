package ru.iitdgroup.tests.webdriver.scoringmodels;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleSpoiler;

public class ScoringModelRecord extends AbstractEdit<ScoringModelRecord> {

    public ScoringModelRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public RuleSpoiler<ScoringModelRecord> openRule(String name) {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::*[text()='%s']", name))
                .click();
        sleep(1);
        return new RuleSpoiler<>(driver, name, getSelf());
    }

    @Override
    public ScoringModelRecord getSelf() {
        return this;
    }
}
