package ru.iitdgroup.tests.webdriver.alerts;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class Alerts extends AbstractView<Alerts> implements TabledView {

    public Alerts(RemoteWebDriver driver) {
        super(driver);
    }

    public AlertRecord openLastRecord() {
        driver.findElementByXPath("//div[@class='panelTable af_table']/table[@class='af_table_content']/tbody/tr[2]").click();
        waitUntil("//img[@title='New Case']");
        return new AlertRecord(driver);
    }

    @Override
    protected Alerts getSelf() {
        return this;
    }
}
