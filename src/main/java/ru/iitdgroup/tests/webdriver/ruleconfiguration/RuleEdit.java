package ru.iitdgroup.tests.webdriver.ruleconfiguration;


import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

/**
 * Контекст для редактирования правил.
 */
public class RuleEdit extends AbstractEdit<RuleEdit> {

    public RuleEdit(RemoteWebDriver driver) {
        super(driver);
    }

    public Rules save() {
        driver.findElementByXPath("//a[@id='btnSave']").click();
        waitUntil("//img[@class='ToolbarButton editRuleMain']");
        return new Rules(driver);
    }

    @Override
    public RuleEdit getSelf() {
        return this;
    }
}
