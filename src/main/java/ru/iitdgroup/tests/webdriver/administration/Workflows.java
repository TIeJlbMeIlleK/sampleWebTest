package ru.iitdgroup.tests.webdriver.administration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class Workflows extends AbstractView<Workflows> {

    public Workflows(RemoteWebDriver driver) {
        super(driver);
    }

    public WorkflowRecord openRecord(String name) {
        driver.findElementByLinkText(name).click();
        waitUntil("//div[text()='Actions']");
        return new WorkflowRecord(driver);
    }

    @Override
    public Workflows getSelf() {
        return this;
    }
}
