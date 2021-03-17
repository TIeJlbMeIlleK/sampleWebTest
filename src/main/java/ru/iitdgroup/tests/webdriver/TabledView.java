package ru.iitdgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleRecord;

/**
 * Вьюшка, которая содержит таблицу
 * Позволяет работать с фильтрами
 */
public interface TabledView<S extends AbstractView> {

    default void clearTableFilters() {
        getSelf().getDriver().findElementsByClassName("filterRemoveRow")
                .forEach(webElement -> {
                    webElement.click();
                    getSelf().sleep(1);
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

        try {
            getSelf().icxpath().element("Value").following(ICXPath.WebElements.INPUT).type(value);
        } catch (NoSuchElementException e) {
            getSelf().getDriver().findElementByXPath("//*[@id=\"custom_tableReportFilters:0:custom_cmbValue\"]").sendKeys(value);
        }
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
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath(String.format("%s/../../../..//following::input[@type='checkbox']", getGroupElement(group))).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath(String.format("%s/../../../..//following::img[@title='Detach']", getGroupElement(group))).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath("//button[2]/span[text()='Yes']").click();
        getSelf().sleep(3);
        return getSelf();
    }

    default S detachAll(String group) {
        getSelf().getDriver().findElementByXPath("//*[@id=\"layoutListTablePanel:j_id2488\"]/table[2]/tbody/tr[1]/th[1]/div/input").click();
        getSelf().getDriver().findElementByXPath("//*[@id=\"j_id2356:1:j_id2401\"]").click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath("/html/body/div[16]/div[3]/div/button[2]/span").click();
        getSelf().sleep(1);
        return getSelf();
    }

    default S detachDelGroup() {

        if (getSelf().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            return getSelf();
        } else {
            getSelf().getDriver().findElementByXPath("//*[@id='layoutListTablePanel:j_id2488']/table[2]/tbody/tr[1]/th[1]/div/input").click();
            getSelf().getDriver().findElementByXPath("//img[@title='Detach']").click();
            getSelf().getDriver().findElementByXPath("//button[2]/span[text()='Yes']").click();
        }
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

    default S attachTransactionIR03(String group, String typeTransaction) {
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        getSelf().waitUntil("//*[@title='Refresh']");
        refreshTable();
        getSelf().sleep(2);
        if (typeTransaction == "Платеж по QR-коду через СБП") {
            getSelf().getDriver().findElementByXPath("//span[text()='Платеж по QR-коду через СБП']").click();
        } else if (typeTransaction == "Перевод по номеру телефона") {
            getSelf().getDriver().findElementByXPath("//span[text()='Перевод по номеру телефона']").click();
        } else {
            getSelf().getDriver().findElementByXPath("//span[text()='Перевод на карту другому лицу']").click();
        }
        getSelf().getDriver().findElementByXPath("//span[text()='Select']").click();
        getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
        return getSelf();
    }


    default S attachOpen() {
        getSelf().getDriver().findElementByXPath("//img[@title='Attach']").click();
        getSelf().waitUntil("//*[@title='Refresh']");
        refreshTable();
        return getSelf();
    }

    default S addAttachMask(String mask) {
        getSelf().getDriver().findElementByXPath("//img[@title='Attach']").click();
        getSelf().waitUntil("//*[@title='Refresh']");
        refreshTable();
        setTableFilter("Маска счёта", "Equals", mask).refreshTab();
        if (getSelf().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            getSelf().getDriver().findElementByXPath("//img[@title='New (Rule_tables) Маски счётов для правила G24']").click();
            getSelf().getDriver().findElementByXPath("//*[@id=\"j_id169:0:j_id172:1:field_col0\"]").sendKeys(mask);
            getSelf().getDriver().findElementByXPath("//a[@id='btnSave']").click();
        } else {
            getSelf().getDriver().findElementByXPath("//*[@class='af_tableSelectMany_cell-icon-format OraTableBorder1111']").click();
            getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
            getSelf().waitUntil("//a[@id='btnEdit']");
        }
        return getSelf();
    }

    default S attachAddTransactionType(String type, String field) {
        getSelf().getDriver().findElementByXPath("//img[@title='Attach']").click();
        getSelf().waitUntil("//*[@title='Refresh']");
        refreshTable();
        setTableFilter(field, "Equals", type).refreshTab();
        if (getSelf().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            getSelf().getDriver().findElementByXPath("//img[@id='j_id106:1:j_id151']").click();
            getSelf().getDriver().findElementByXPath("//*[@id=\"j_id169:0:j_id172:1:field_col0\"]").sendKeys(type);
            getSelf().getDriver().findElementByXPath("//a[@id='btnSave']").click();
        } else {
            getSelf().getDriver().findElementByXPath("//*[@class='af_tableSelectMany_cell-icon-format OraTableBorder1111']").click();
            getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
            getSelf().waitUntil("//a[@id='btnEdit']");
        }
        return getSelf();
    }

    default S attachVESCode46(String group) {
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        getSelf().waitUntil("//*[@title='Refresh']");
        refreshTable();
        getSelf().sleep(2);
        getSelf().getDriver().findElementByXPath("//span[text()='Вход в ДБО']").click();
        getSelf().getDriver().findElementByXPath("//span[text()='Select']").click();
        getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
        getSelf().waitUntil("//a[@id='btnEdit']");
        return getSelf();
    }

    default S attachTransaction(String group, String tranType, String summ) {
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//*[@id=\"j_id2356:0:j_id2401\"]")).click();
        getSelf().getDriver().findElementByXPath("//*[@id=\"j_id169:0:j_id172:2:btnSelect_field_col1\"]").click();
        getSelf().getDriver().findElementByXPath("//span[text()='" + tranType + "']").click();
        getSelf().getDriver().findElementByXPath("//*[@id=\"btnSelectLookup\"]/span").click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath("//*[@id=\"j_id142:0:j_id145:2:field_col0\"]").sendKeys(summ);
        getSelf().getDriver().findElementByXPath("//*[@id=\"btnSave\"]").click();
        return getSelf();
    }

    default S detachWithoutRecording(String group) {
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElements(By.xpath("//a[text()='Show All']"))
                .forEach(WebElement::click);
        getSelf().sleep(1);
        if (getSelf().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            return getSelf();
        }
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//input[@type='checkbox']")).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Detach']")).click();
        getSelf().sleep(1);
        getSelf().getDriver().findElementByXPath("//button[2]/span[text()='Yes']").click();
        getSelf().sleep(1);
        return getSelf();
    }

    default S attachAddingValue(String group, String field, String operator, String value) {
        getSelf().getDriver().findElementByXPath(getGroupElement(group)).findElement(By.xpath("//img[@title='Attach']")).click();
        getSelf().waitUntil("//*[@title='Refresh']");
        clearTableFilters();
        setTableFilter(field, operator, value);
        refreshTable();
        getSelf().sleep(1);
        for (WebElement webElement : getSelf().getDriver().findElementsByXPath("//a[text()='Show All']")) {
            webElement.click();
        }
        getSelf().sleep(1);
        getSelf().getDriver().executeScript("window.scrollTo(0, 10000)");
        if (getSelf().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() == 0) {
            getSelf().getDriver().findElementByXPath("//*[@class='af_column_header-icon-format']//input[1]").click();
            getSelf().getDriver().findElementByXPath("//a[@title='OK']").click();
        } else {
            // отсутствуют записи, создаём новую
            getSelf().getDriver().findElements(By.className("toolbarCell")).get(1).click();
            getSelf().sleep(1);
            WebElement input = getSelf().getDriver()
                    .findElementByXPath("//span[@class='moduleDetailsText af_inputText']")
                    .findElements(By.tagName("input"))
                    .get(0);
            input.click();
            input.clear();
            input.click();
            input.sendKeys(value);
            getSelf().getDriver().findElementByXPath("//a[@id='btnSave']").click();
        }
        getSelf().waitUntil("//a[@id='btnEdit']");

        return getSelf();
    }

    default String getGroupElement(String group) {
        return String.format("//div[@class='%s' and text()='%s']", "customTitle ellipsisContent", group);
    }

    S getSelf();

}