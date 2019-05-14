package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTableEdit<T extends ICView> extends ICView<T>{

    private static final String SAVE_ACTION = "Save";


    public ReferenceTableEdit(RemoteWebDriver driver) {
       super(driver);
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    public ReferenceTableEdit<T> fillMasked(AllFields field, String fieldText) {
        final WebElement element;
        icxpath()
                .element(field.heading)
                .following(ICXPath.WebElements.INPUT)
                .type(fieldText);
        return this;
    }

    public ReferenceTableRecord<T> save() {
        driver.findElement(By.linkText(SAVE_ACTION)).click();
        return  new ReferenceTableRecord<>(driver);
    }

}
