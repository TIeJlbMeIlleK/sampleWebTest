package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTableDetails {

    private static final String SAVE_ACTION = "Save";

    private final RemoteWebDriver driver;

    public ReferenceTableDetails(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    public ReferenceTableDetails fillMasked(String fieldName, String fieldText) {
        final WebElement field;
        field = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='" + fieldName + "'])[1]/following::input[1]"));
        field.clear();
        field.sendKeys(fieldText);
        return this;
    }

    public void save() {
        driver.findElement(By.linkText(SAVE_ACTION)).click();
    }

}
