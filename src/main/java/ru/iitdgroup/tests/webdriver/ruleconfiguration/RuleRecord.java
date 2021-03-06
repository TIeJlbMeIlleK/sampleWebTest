package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.By;
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
        driver.findElementByXPath(getGroupElement(group)).findElements(By.xpath("//a[text()='Show All']"))
                .forEach(WebElement::click);
        sleep(1);
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//input[@type='checkbox']")).click();
        sleep(1);
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Detach']")).click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(1);
        return getSelf();
    }

    public RuleRecord attach(String group, String field, String operator, String value) {
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        sleep(1);
        for (WebElement webElement : driver.findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        sleep(1);
        driver.executeScript("window.scrollTo(0, 10000)");
        driver.findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
        driver.findElementByXPath("//a[@title='OK']").click();
        waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    public RuleRecord detachWithoutRecording(String group) {
        driver.findElementByXPath(getGroupElement(group)).findElements(By.xpath("//a[text()='Show All']"))
                .forEach(WebElement::click);
        sleep(1);
        if (driver.findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            return getSelf();
        }

        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//input[@type='checkbox']")).click();
        sleep(1);
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Detach']")).click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(1);
        return getSelf();
    }

    public RuleRecord attachAddingValue(String group, String field, String operator, String value) {
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        sleep(1);
        for (WebElement webElement : driver.findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        sleep(1);
        driver.executeScript("window.scrollTo(0, 10000)");
        if (driver.findElementsByXPath("//*[text()='No records were found.']").size() == 0) {
            driver.findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
            driver.findElementByXPath("//a[@title='OK']").click();
        } else {
            // ?????????????????????? ????????????, ?????????????? ??????????
            driver.findElements(By.className("toolbarCell")).get(1).click();
            sleep(1);
            WebElement input = getSelf().getDriver()
                    .findElementByXPath("//span[@class='moduleDetailsText af_inputText']")
                    .findElements(By.tagName("input"))
                    .get(0);
            input.click();
            input.clear();
            input.click();
            input.sendKeys(value);
            driver.findElementByXPath("//a[@id='btnSave']").click();
        }
        waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    public RuleRecord attachVESCode46(String group) {
        driver.findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        waitUntil("//*[@title='Refresh']");
        refreshTable();
        sleep(2);
        driver.findElementByXPath("//span[text()='???????? ?? ??????']").click();
        driver.findElementByXPath("//span[text()='Select']").click();
        driver.findElementByXPath("//a[@title='OK']").click();
        waitUntil("//a[@id='btnEdit']");
        return getSelf();
    }

    public String getGroupElement(String group) {
        return String.format("//div[@class='%s' and text()='%s']", "customTitle ellipsisContent", group);
    }

    @Override
    public RuleRecord getSelf() {
        return this;
    }
}
