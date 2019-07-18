package ru.iitdgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

import static java.lang.Thread.sleep;

/**
 * Вьюшка, которая содержит таблицу
 * Позволяет работать с фильтрами
 */
public interface TabledView<S> {

    RemoteWebDriver getDriver();

    ICXPath icxpath();

    default void clearTableFilters() {
        getDriver().findElementsByClassName("filterRemoveRow")
                .forEach(webElement -> {
                    webElement.click();
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
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
        try {
            sleep(2000);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return getSelf();
    }

    S getSelf();

}
