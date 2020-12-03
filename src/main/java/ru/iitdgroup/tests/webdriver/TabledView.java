package ru.iitdgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

/**
 * Вьюшка, которая содержит таблицу
 * Позволяет работать с фильтрами
 */
public interface TabledView<S extends AbstractView> {

    default void clearTableFilters() {
        getSelf().getDriver().findElementsByClassName("filterRemoveRow")
                .forEach(webElement -> {
                    webElement.click();
                    getSelf().sleep(2);
                });
    }

    default S setTableFilter(String field, String operator, String value) {
        clearTableFilters();
        getSelf().getDriver().findElementByXPath("//*[text()='Add Filter']").click();
        getSelf().sleep(2);

        Select columnField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(0));
        columnField.selectByVisibleText(field);
        getSelf().sleep(2);
        Select operatorField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(1));
        operatorField.selectByVisibleText(operator);

        getSelf().sleep(3);

//        getSelf().icxpath().element("Value").following(ICXPath.WebElements.INPUT).type(value);
        getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:0:custom_cmbValue\"]").sendKeys(value);

        return getSelf();
    }

    default S setTableFilterForTransactions(String field, String operator, String value) {
        clearTableFilters();
        getSelf().getDriver().findElementByXPath("//*[text()='Add Filter']").click();
        getSelf().sleep(2);

        Select columnField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(0));
        columnField.selectByVisibleText(field);
        getSelf().sleep(2);
        Select operatorField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(1));
        operatorField.selectByVisibleText(operator);

        getSelf().sleep(3);

        getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:0:custom_cmbValue\"]").sendKeys(value);

        return getSelf();
    }

    default S setNewTableFilterForTransactions(String field, String operator, String value) {
        getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:custom_btnReportAddFilter\"]").click();
        getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:1:cmdMoveUp\"]/img").click();
        getSelf().sleep(2);

        Select columnField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(0));
        columnField.selectByVisibleText(field);
        getSelf().sleep(2);
        Select operatorField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(1));
        operatorField.selectByVisibleText(operator);

        getSelf().sleep(3);

        getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:0:custom_cmbValue\"]").sendKeys(value);

        return getSelf();
    }

    //FIXME поправить в случае поля Active (пример Список клиентов)
    default S setTableFilterWithActive(String field, String operator, String value) {
        clearTableFilters();
        getSelf().getDriver().findElementByXPath("//*[text()='Add Filter']").click();
        getSelf().sleep(2);

        Select columnField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(0));
        columnField.selectByVisibleText(field);
        getSelf().sleep(2);
        Select operatorField = new Select(getSelf().getDriver()
                .findElementByXPath("//div[@class='dataSetFiltersTable af_table']")
                .findElements(By.className("af_selectOneChoice_content"))
                .get(1));
        operatorField.selectByVisibleText(operator);

        getSelf().sleep(3);

        WebElement valueInput = getSelf().getDriver().findElementByXPath("//*[@id='custom_tableReportFilters']//following::input[2]");
        valueInput.click();
        valueInput.clear();
        valueInput.click();
        valueInput.sendKeys(value);

        return getSelf();
    }

    default S refreshTable() {
        getSelf().getDriver().findElementByXPath("//img[@title='Refresh']").click();
        getSelf().sleep(2);

        return getSelf();
    }

    default S runReport() {
        getSelf().getDriver().findElementByXPath("//img[@title='Run Report']").click();
        getSelf().sleep(2);

        return getSelf();
    }

    default S detach(String group) {
        boolean exist = true;
        for (WebElement webElement : getSelf().getDriver().findElementsByXPath(String.format("%s/../../../..//a[text()='Show All']", getGroupElement(group)))) {
            webElement.click();
            exist = false;
        }
        if (!exist) {
            return getSelf();
        }
        getSelf().sleep(3);
        getSelf().getDriver().findElementByXPath(String.format("%s/../../../..//following::input[@type='checkbox']", getGroupElement(group))).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath(String.format("%s/../../../..//following::img[@title='Detach']", getGroupElement(group))).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath("//button[2]/span[text()='Yes']").click();
        getSelf().sleep(3);
        return getSelf();
    }

    default S attach(String group, String field, String operator, String value) {
        getSelf().getDriver().findElementByXPath(String.format("%s/../../../..//img[@title='Attach']", getGroupElement(group))).click();
        getSelf().waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        getSelf().sleep(2);
        for (WebElement webElement : getSelf().getDriver().findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        getSelf().sleep(2);
        getSelf().getDriver().executeScript("window.scrollTo(0, 10000)");
        getSelf().getDriver().findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
        getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
        getSelf().waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    default String getGroupElement(String group) {
        return String.format("//div[@class='%s' and text()='%s']", "customTitle ellipsisContent", group);
    }

    S getSelf();

}