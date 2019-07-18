package ru.iitdgroup.tests.webdriver.scoringmodels;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class ScoringModels extends AbstractView<ScoringModels> {

    public ScoringModels(RemoteWebDriver driver) {
        super(driver);
    }

    public ScoringModelRecord openRecord(String name) {
        driver.findElementByLinkText(name).click();
        waitUntil("//*[@id='btnAddIncident']");
        return new ScoringModelRecord(driver);
    }

    @Override
    public ScoringModels getSelf() {
        return this;
    }
}
