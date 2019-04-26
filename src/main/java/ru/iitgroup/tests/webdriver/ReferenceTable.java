package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static ru.iitgroup.tests.webdriver.InvestigationCenter.driver;

public class ReferenceTable {

    private static final String SAVE_ACTION = "Save";

    public static void fillText(String fieldName, String fieldText) {
        final WebElement field;
        field = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='" + fieldName + "'])[1]/following::input[1]"));
        field.clear();
        field.sendKeys(fieldText);
    }

    public static void addRecord() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]")).click();
    }

    public static void save() {
        driver.findElement(By.linkText(SAVE_ACTION)).click();
    }
}
