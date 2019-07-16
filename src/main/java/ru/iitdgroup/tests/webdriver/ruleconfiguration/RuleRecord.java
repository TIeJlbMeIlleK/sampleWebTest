package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class RuleRecord extends AbstractEdit<RuleRecord> implements TabledView {

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
        for (WebElement select : driver.findElementsByXPath("//input[@title='Select']")) {
            select.click();
        }
        driver.findElementByXPath("//a[@title='OK']").click();
        waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    @Override
    protected RuleRecord getSelf() {
        return this;
    }
}
