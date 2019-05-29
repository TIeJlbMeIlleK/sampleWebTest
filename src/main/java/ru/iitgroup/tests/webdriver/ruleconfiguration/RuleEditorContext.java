package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.ICView;

/**
 * Контекст для редактирования правил.
 */
public class RuleEditorContext extends ICView<RuleEditorContext> {

    public RuleEditorContext(RemoteWebDriver driver) {
        super(driver);
    }

    public RuleEditorContext setName(String name) {
        WebElement inputNameField = driver.findElementByXPath(
                "//span[text()='Name:']/parent::td/following-sibling::td[position()=1]//input"
        );
        inputNameField.clear();
        inputNameField.sendKeys(name);

        return this;
    }

    public RuleEditorContext active() {
        driver.findElementByXPath("//span[text()='Active:']/parent::td/following-sibling::td[position()=1]//input").click();

        return this;
    }

    public RuleEditorContext notActive() {
        return active();
    }

    public RuleEditorContext test() {
        driver.findElementByXPath("//span[text()='isTest:']/parent::td/following-sibling::td[position()=1]//input").click();

        return this;
    }

    public RuleEditorContext notTest() {
        return test();
    }

    public RuleEditorContext setDescription(String description) {
        WebElement inputNameField = driver.findElementByXPath(
                "//span[text()='Name:']/parent::td/following-sibling::td[position()=1]//textarea"
        );
        inputNameField.clear();
        inputNameField.sendKeys(description);

        return this;
    }
}
