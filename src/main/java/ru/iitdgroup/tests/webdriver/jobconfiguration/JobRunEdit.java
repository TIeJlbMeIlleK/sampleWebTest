package ru.iitdgroup.tests.webdriver.jobconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class JobRunEdit extends AbstractEdit<JobRunEdit> {

    private static final String DESCRIPTION_TEXT_AREA = "Description";
    private static final String PARAMETERS_TEXT_AREA = "Parameters";

    private final RemoteWebDriver driver;

    public JobRunEdit(RemoteWebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    @Override
    protected JobRunEdit getSelf() {
        return this;
    }

    public JobRunEdit run() {
        driver.findElementByXPath("//button[@type='submit']").click();

        sleep(2);

        return new JobRunEdit(driver);
    }

    public Jobs runWithDescriptionAndParameters(String description, String... parameters) {
        fillTextArea(DESCRIPTION_TEXT_AREA, description);
        fillTextArea(PARAMETERS_TEXT_AREA, joinParameters(parameters));

        run();

        return new Jobs(driver);
    }

    public Jobs runWithParameters(String... parameters) {
        return runWithDescriptionAndParameters("", parameters);
    }

    private String joinParameters(String... parameters) {
        return String.join("\n", parameters);
    }
}
