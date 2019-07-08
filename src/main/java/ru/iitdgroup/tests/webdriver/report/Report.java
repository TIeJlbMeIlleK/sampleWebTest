package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.importruletable.ImportRuleTable;

public class Report extends AbstractView<Report> {

    public Report(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected Report getSelf() {
        return this;
    }
}
