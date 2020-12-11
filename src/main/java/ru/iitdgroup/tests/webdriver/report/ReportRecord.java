package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;
import org.openqa.selenium.WebElement;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleRecord;

import java.util.List;


public class ReportRecord extends AbstractView<ReportRecord> implements TabledView<ReportRecord> {

    public ReportRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public ReportDetail openFirst() {
        driver.findElementByXPath("//div[@class='panelTable af_table']/table[@class='af_table_content']/tbody/tr[2]")
                .click();
//        waitUntil("//a[@id='btnEdit']");
        return new ReportDetail(driver);
    }


    public ReportRecord changeAllFiltersCheckboxes() {
        List<WebElement> elements = driver.findElementsByXPath("//input[starts-with(@id, 'custom_tableReportFilters:')]");
        elements.forEach(webElement -> {
            if (webElement.getAttribute("id").endsWith("j_id287")) {
                webElement.click();
                sleep(1);
            }
        });
        return getSelf();
    }

    public ReportRecord removeAllFilters() { //custom_tableReportFilters:0:cmdDelete
        List<WebElement> elements = driver.findElementsByXPath("//div[@id='custom_tableReportFilters']/table/tbody/tr");
        int filtersCount = elements.size() - 2;
        for (int i = 0; i < filtersCount; i++) {
            driver.findElementByXPath("//a[starts-with(@id, 'custom_tableReportFilters:0')]").click();
            sleep(1);
        }
        return getSelf();
    }

    /**
     * включает Run Report
     *
     * @return
     */
    public ReportRecord turnOnRunReport() {
        driver.findElementByXPath("//td[@class='toolbarCell']//a/img[@class='ToolbarButton runReport_style']").click();
        sleep(3);
        return getSelf();
    }


    public ReportRecord setTableFilter(String field, String operator, String value) {
        removeAllFilters();
        driver.findElementById("custom_btnReportAddFilter").click();
        sleep(1);

        Select fieldName = new Select(
                driver.findElementByXPath("//div[@id='custom_tableReportFilters']//select[@id='custom_tableReportFilters:0:custom_cmbField']"));
        String s = fieldName.getFirstSelectedOption().getText();
        fieldName.selectByVisibleText(field);
        sleep(1);

        Select operatorField = new Select(
                driver.findElementByXPath("//select[@id='custom_tableReportFilters:0:custom_cmbOperator']"));
        operatorField.selectByVisibleText(operator);
        sleep(1);

        Select valueField = new Select(
                driver.findElementByXPath("//select[@id='custom_tableReportFilters:0:selectListValue']"));
        valueField.selectByVisibleText(value);
        //    getSelf().
        sleep(1);

        turnOnRunReport();
        return getSelf();
    }

    @Override
    public ReportRecord getSelf() {
        return this;
    }

    public String getLastRecordIdentificator() {
        return driver.findElementByXPath("//div[@class='panelTable af_table']/table/tbody/tr[2]/td[8]//span").getText();
    }

    public String getLastRecordSuccess() {
        return driver.findElementByXPath("//div[@class='panelTable af_table']/table/tbody/tr[2]/td[11]").getText();
    }
}
