package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTableRecord<T extends ICView> extends ICView<T> {
    public ReferenceTableRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public ReferenceTableEdit<ReferenceTableEdit> edit(){
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        return new ReferenceTableEdit<>(driver);
    }
}
