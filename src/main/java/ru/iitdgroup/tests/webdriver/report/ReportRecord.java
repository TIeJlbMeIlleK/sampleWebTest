package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class ReportRecord extends AbstractView<ReportRecord> implements TabledView<ReportRecord> {

    public ReportRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public ReportDetail openFirst() {
        driver.findElementByXPath("//div[@class='panelTable af_table']/table[@class='af_table_content']/tbody/tr[2]")
                .click();
        waitUntil("//a[@id='btnEdit']");
        return new ReportDetail(driver);
    }

    @Override
    public ReportRecord getSelf() {
        return this;
    }
}
