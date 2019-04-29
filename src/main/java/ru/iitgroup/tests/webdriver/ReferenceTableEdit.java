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

    public ReferenceTableEdit fillMasked(AllFields field, String fieldText) {
        final WebElement element;
        element = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='" + field.heading+ "'])[1]/following::input[1]"));
        element.clear();
        element.sendKeys(fieldText);
        return this;
    }

    public void save() {
        driver.findElement(By.linkText(SAVE_ACTION)).click();
    }

}
