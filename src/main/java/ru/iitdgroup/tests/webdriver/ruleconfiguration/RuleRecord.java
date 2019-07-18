package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class RuleRecord extends AbstractEdit<RuleRecord> implements TabledView<RuleRecord> {

    private final RemoteWebDriver driver;

    public RuleRecord(RemoteWebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    public RuleEdit edit() {
        driver.findElementByXPath("//a[@id='btnEdit']").click();
        waitUntil("//a[@id='btnSave']");
        return new RuleEdit(driver);
    }

    public RuleRecord tab(String tab) {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::tr[@class='menuTabs']//following::*[contains(text(), '%s')]", tab))
                .click();
        sleep(1);
        return getSelf();
    }

    public RuleSpoiler openSpoiler(String name) {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::*[text()='%s']", name))
                .click();
        sleep(1);
        return new RuleSpoiler<>(driver, name, getSelf());
    }

    @Override
    public RuleRecord getSelf() {
        return this;
    }
}
