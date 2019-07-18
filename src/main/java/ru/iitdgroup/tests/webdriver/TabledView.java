package ru.iitdgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

import static java.lang.Thread.sleep;

/**
 * Вьюшка, которая содержит таблицу
 * Позволяет работать с фильтрами
 */
public interface TabledView<S extends AbstractView> {

    RemoteWebDriver getDriver();

    ICXPath icxpath();

    default void clearTableFilters() {
        getDriver().findElementsByClassName("filterRemoveRow")
                .forEach(webElement -> {
                    webElement.click();
                    getSelf().sleep(2);
                });
    }

    default void setTableFilter(String field, String operator, String value) {
        try {
            clearTableFilters();
            getDriver().findElementById("base_btnReportAddFilter").click();
            sleep(2000);

            Select columnField = new Select(getDriver()
                    .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                    .findElements(By.className("af_selectOneChoice_content"))
                    .get(0));
            columnField.selectByVisibleText(field);
            sleep(2000);
            Select operatorField = new Select(getDriver()
                    .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                    .findElements(By.className("af_selectOneChoice_content"))
                    .get(1));
            operatorField.selectByVisibleText(operator);

            icxpath()
                    .element("Value")
                    .following(ICXPath.WebElements.INPUT)
                    .type(value);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    default S refreshTable() {
        getDriver().findElementByXPath("//img[@title='Refresh']").click();
        getSelf().sleep(2);

        return getSelf();
    }

    default S detach(String group) {
        getGroupElement(group).findElement(By.xpath("//a[text()='Show All']")).click();
        getSelf().sleep(3);
        getGroupElement(group).findElement(By.xpath("//input[@type='checkbox']")).click();
        getSelf().sleep(1);
        getGroupElement(group).findElement(By.xpath("//img[@title='Detach']")).click();
        getSelf().sleep(1);
        getDriver().findElementByXPath("//button[2]/span[text()='Yes']").click();
        getSelf().sleep(3);
        return getSelf();
    }

    default S attach(String group, String field, String operator, String value) {
        getGroupElement(group).findElement(By.xpath("//img[@title='Attach']")).click();
        getSelf().waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        getSelf().sleep(2);
        for (WebElement webElement : getDriver().findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        getSelf().sleep(2);
        getDriver().executeScript("window.scrollTo(0, 10000)");
        getDriver().findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
        getDriver().findElementByXPath("//a[@title='OK']").click();
        getSelf().waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    default WebElement getGroupElement(String group) {
        return getDriver().findElementByXPath(String.format("//div[@class='%s' and text()='%s']", "customTitle ellipsisContent", group));
    }

    S getSelf();

}
