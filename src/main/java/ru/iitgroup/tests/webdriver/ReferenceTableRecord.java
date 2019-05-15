package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTableRecord extends ICView {
    public ReferenceTableRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public ReferenceTableEdit edit(){
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        return new ReferenceTableEdit(driver);
    }

    @Override
    public ReferenceTableRecord selectVisible() {
        super.selectVisible();
        return this;
    }

    @Override
    public ReferenceTableRecord sleep(double seconds) {
        super.sleep(seconds);
        return this;
    }
}
