package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTable {


    private final RemoteWebDriver driver;

    public ReferenceTable(RemoteWebDriver driver) {
        this.driver = driver;
    }


    public ReferenceTableDetails addRecord() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]")).click();
        return new ReferenceTableDetails(driver);
    }

}
