package ru.iitdgroup.tests.webdriver.alerts;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class AlertRecord extends AbstractEdit<AlertRecord> {

    public AlertRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public AlertRecord action(String action) {
        driver.findElementByXPath("//*[text()='Actions']").click();
        driver.findElementByXPath(String.format("//div[@class='qtip-content']//following::a[text()='%s']", action)).click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(3);
        return getSelf();
    }

    @Override
    public AlertRecord getSelf() {
        return this;
    }
}
