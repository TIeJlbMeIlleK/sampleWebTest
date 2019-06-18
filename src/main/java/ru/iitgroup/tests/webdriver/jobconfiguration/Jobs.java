package ru.iitgroup.tests.webdriver.jobconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractView;

public class Jobs extends AbstractView<Jobs> {

    public Jobs(RemoteWebDriver driver) {
        super(driver);
    }

    public JobRunEdit selectJob(String jobName) {
        String jobListPosition = String.format("//div[@title='%s']", jobName);
        driver.findElementByXPath(jobListPosition + "[1]/following::button[1]").click();
        driver.findElementByXPath(jobListPosition + "[1]/following::a[@data-action-id='RUN']").click();
        waitUntil("//textarea[@id='parameters']");

        return new JobRunEdit(driver);
    }

    @Override
    protected Jobs getSelf() {
        return this;
    }
}
