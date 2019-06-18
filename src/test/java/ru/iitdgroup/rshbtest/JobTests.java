package ru.iitdgroup.rshbtest;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import static org.testng.Assert.fail;

public class JobTests extends RSHBTests {

    @Test(description = "Пример запуска job." +
            " Кинет InvalidElementStateException, если статус Job после указанного ожидания будет отличаться от Success")
    public void runJob() {
        try {
            ic.locateJobs()
                    .selectJob("geoipupdater")
                    .addParameter("key1", "value1")
                    .addParameter("key2", "value2")
                    .description("description 1")
                    .waitSeconds(10)
                    .waitStatus(JobRunEdit.JobStatus.STOPPED)
                    .run();
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            fail(message);
        } finally {
            ic.getDriver().close();
        }
    }

}
