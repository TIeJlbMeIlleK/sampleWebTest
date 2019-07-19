package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class ReportDetail extends AbstractView<ReportDetail> {

    public ReportDetail(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public ReportDetail getSelf() {
        return this;
    }
}
