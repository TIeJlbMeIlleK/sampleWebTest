package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.AllRules;
import ru.iitgroup.tests.webdriver.ic.AbstractView;

/**
 * Контекст для работы с экранной формой правил.
 */
public class Rules extends AbstractView<Rules> {

    public Rules(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected Rules getSelf() {
        return this;
    }

    public RuleEdit createRule(AllRules template) {
        driver.findElementByXPath("//div[@id='toolbarActions']//td[@class='toolbarCell']//*[contains(@class,'newRule')]").click();
        waitUntil("//input[@id='ruleTemplateSearchText']");
        driver.findElementByXPath("//input[@id='ruleTemplateSearchText']").click();

        driver.findElementByXPath(String.format("//a[contains(text(),'%s')]", template.name())).click();
        waitUntil("//button[text()='OK']").click();
        return new RuleEdit(driver);
    }

    public Rules selectRule(String heading) {
        //language=XPath
        final String xpath = ".//*[normalize-space(text())='" + heading + "'][1]/preceding::input[2][@type='checkbox']";


        final String ruleName = String.format(".//*[text()='%s'][1]/preceding::input[2][@type='checkbox']",heading);
        driver.findElementByXPath(ruleName).click();
        return this;
    }

    public Rules activate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Activate']")).click();
        return this;
    }

    public Rules deactivate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Deactivate']")).click();
        waitUntil("//*[contains(text(),'Operation succeeded')]");
        return this;
    }

    public enum Action {
        CHANGE_WORKSPACE(1),
        DELETE(2),
        ACTIVATE(3),
        DEACTIVATE(4);

        private final int pos;

        Action(int pos) {
            this.pos = pos;
        }
    }
}