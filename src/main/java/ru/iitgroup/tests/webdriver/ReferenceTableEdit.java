package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTableEdit extends ICView{

    private static final String SAVE_ACTION = "Save";


    public ReferenceTableEdit(RemoteWebDriver driver) {
       super(driver);
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    public ReferenceTableRecord fillMasked(String fieldName, String fieldText) {
        final WebElement field;
        field = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='" + fieldName + "'])[1]/following::input[1]"));
        field.clear();
        field.sendKeys(fieldText);
        return new ReferenceTableRecord( driver);
    }

    public void save() {
        driver.findElement(By.linkText(SAVE_ACTION)).click();
    }

}
