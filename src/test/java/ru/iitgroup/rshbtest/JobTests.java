package ru.iitgroup.rshbtest;

import org.testng.annotations.Test;

import static org.testng.Assert.fail;

public class JobTests extends RSHBTests {

    private static final String DESCRIPTION = "description 1";
    private static final String[] PARAMS = new String[]{"param 1", "param 2"};

    @Test(description = "Пример запуска job")
    public void runJob() {
        try {
            ic.locateJobs()
                    .selectJob("geoipupdater")
                    .fillTextArea("Parameters",  String.join("\n", PARAMS))
                    .run();
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            fail(message);
        } finally {
            ic.getDriver().close();
        }
    }

    @Test(description = "Пример запуска job с описанием и параметрами")
    public void runJobWithDescriptionAndParameters() {
        try {
            ic.locateJobs()
                    .selectJob("geoipupdater")
                    .runWithDescriptionAndParameters(DESCRIPTION, PARAMS);
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            fail(message);
        } finally {
            ic.getDriver().close();
        }
    }

    @Test(description = "Пример запуска job с параметрами")
    public void runJobWithParameters() {
        try {
            ic.locateJobs()
                    .selectJob("geoipupdater")
                    .runWithParameters(PARAMS);
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            fail(message);
        } finally {
            ic.getDriver().close();
        }
    }

}
