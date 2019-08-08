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

    public RuleRecord detach(String group) {
        getGroupElement(group).findElement(By.xpath("//a[text()='Show All']")).click();
        sleep(3);
        getGroupElement(group).findElement(By.xpath("//input[@type='checkbox']")).click();
        sleep(1);
        getGroupElement(group).findElement(By.xpath("//img[@title='Detach']")).click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(3);
        return getSelf();
    }

    public RuleRecord attach(String group, String field, String operator, String value) {
        getGroupElement(group).findElement(By.xpath("//img[@title='Attach']")).click();
        waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        sleep(2);
        for (WebElement webElement : driver.findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        sleep(2);
        driver.executeScript("window.scrollTo(0, 10000)");
        driver.findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
        driver.findElementByXPath("//a[@title='OK']").click();
        waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    private WebElement getGroupElement(String group) {
        return driver.findElementByXPath(String.format("//div[@class='%s' and text()='%s']", "customTitle ellipsisContent", group));
    }

    @Override
    public RuleRecord getSelf() {
        return this;
    }
}
