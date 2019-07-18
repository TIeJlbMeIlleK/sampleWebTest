package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.WebElement;
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

    public RuleRecord attach(String group, String field, String operator, String value) {
        driver.findElementByXPath(String.format("//div[@class='%s' and text()='%s']//following::img[@title='Attach']", "customTitle ellipsisContent", group))
                .click();
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

    @Override
    public RuleRecord getSelf() {
        return this;
    }
}
