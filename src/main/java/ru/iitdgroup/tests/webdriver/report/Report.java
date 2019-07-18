package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class Report extends AbstractView<Report> {

    public Report(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public Report getSelf() {
        return this;
    }
}
