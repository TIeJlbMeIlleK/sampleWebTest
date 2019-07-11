package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

/**
 * Контекст для работы с экранной формой правил.
 */
public class Rules extends AbstractView<Rules> implements TabledView {

    public Rules(RemoteWebDriver driver) {
        super(driver);
    }

    public RuleEdit createRule(String templateName) {
        driver.findElementByXPath("//div[@id='toolbarActions']//td[@class='toolbarCell']//*[contains(@class,'newRule')]").click();
        waitUntil("//input[@id='ruleTemplateSearchText']");
        driver.findElementByXPath("//input[@id='ruleTemplateSearchText']").click();

        driver.findElementByXPath(String.format("//a[contains(text(),'%s')]", templateName)).click();
        waitUntil("//button[text()='OK']").click();
        return new RuleEdit(driver);
    }

    public RuleEdit editRule(String ruleName) {
        return openRecord(ruleName).edit();
    }

    private RuleRecord openRecord(String ruleName) {
        //FIXME: не работает с правилами в конце списка - в IC некликабельно то, что не помещается полностью на экран
        final String xpath = String.format("//span[@style=' ' and text()='%s']/../..", ruleName);
        driver.findElementByXPath(xpath).click();
        return new RuleRecord(driver);
    }

    public Rules selectRule(String heading) {
        //language=XPath
        final String xpath = ".//*[normalize-space(text())='" + heading + "'][1]/preceding::input[2][@type='checkbox']";

        final String ruleName = String.format(".//*[text()='%s'][1]/preceding::input[2][@type='checkbox']", heading);
        driver.findElementByXPath(ruleName).click();
        return new Rules(driver);
    }

    public Rules activate() {
        executeAction(Action.Activate);
        waitSuccess();
        return this;
    }

    public Rules deactivate() {
        executeAction(Action.Deactivate);
        waitSuccess();
        return this;
    }

    public Rules deleteRule(String heading) {
        selectRule(heading);
        executeAction(Action.Delete);
        sleep(0.5);
        /*
        ищем диалог подтверждения по паре кнопок
         */
        driver.findElementByXPath("//button/span[text()='No']/preceding::button/span[text()='Yes']").click();
        waitUntil("//*[contains(text(),'Operation succeeded') and @class='globalMessagesInfo']");
        return this;
    }

    private void waitSuccess() {
        waitUntil("//div[contains(text(),'Operation succeeded - ') and @class='actionsMessage']");
    }


    protected void executeAction(Action action) {
        driver.findElementByXPath("(.//*[text()='Actions'])[1]/img[1]").click();
        final String xPath = String.format("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='%s']", action.name);
        driver.findElementByXPath(xPath).click();
    }

    public Rules setFilter(String field, String operator, String value) {
        setTableFilter(field, operator, value);
        refreshTable();
        return this;
    }


    protected enum Action {
        Activate("Activate"),
        Deactivate("Deactivate"),
        Delete("Delete"),
        ChangeWorkspace("Change Workspace");

        private final String name;

        Action(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected Rules getSelf() {
        return this;
    }
}