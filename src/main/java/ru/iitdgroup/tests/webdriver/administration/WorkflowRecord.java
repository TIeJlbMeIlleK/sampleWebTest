package ru.iitdgroup.tests.webdriver.administration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class WorkflowRecord extends AbstractEdit<WorkflowRecord> {

    public WorkflowRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public WorkflowAction openAction(String name) {
        driver.findElementByXPath(String.format("//span[text()='%s']", name)).click();
        waitUntil("//a[@id='btnSave']");
        return new WorkflowAction(driver);
    }

    public WorkflowAction addAction() {
        driver.findElementById("btnCreate").click();
        waitUntil("//a[@id='btnSave']");
        return new WorkflowAction(driver);
    }

    public WorkflowRecord selectAction(String name) {
        driver.findElementByXPath(String.format("//span[text()='%s']/preceding::td[1]", name)).click();
        return getSelf();
    }

    public WorkflowRecord deleteAction(String name) {
        selectAction(name);
        driver.findElementById("btnDelete").click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        waitUntil("//li[text()='1 Action/s deleted.']");
        return getSelf();
    }

    @Override
    public WorkflowRecord getSelf() {
        return this;
    }
}
