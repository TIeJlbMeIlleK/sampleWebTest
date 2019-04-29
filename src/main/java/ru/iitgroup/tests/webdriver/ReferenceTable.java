package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTable extends ICView{



    public ReferenceTable(RemoteWebDriver driver) {
        super( driver);

    }

    public ReferenceTableEdit addRecord() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]")).click();
        return new ReferenceTableEdit(driver);
    }

}
