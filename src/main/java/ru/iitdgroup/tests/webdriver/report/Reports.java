package ru.iitdgroup.tests.webdriver.report;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class Reports extends AbstractView<Reports> {

    public Reports(RemoteWebDriver driver) {
        super(driver);
    }

    public Reports openFolder(String folderName) {
        driver.findElementByXPath(String.format("//*[@class='sideBarText' and text()='%s']", folderName)).click();
        sleep(2);
        return getSelf();
    }

    public ReportRecord openRecord(String name) {
        driver.findElementByLinkText(name).click();
        waitUntil("//*[@title='Run Report']");
        return new ReportRecord(driver);
    }

    @Override
    public Reports getSelf() {
        return this;
    }
}
