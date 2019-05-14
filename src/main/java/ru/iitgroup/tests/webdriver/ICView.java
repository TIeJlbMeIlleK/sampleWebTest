package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public abstract class  ICView <RealView extends ICView> {
    protected final RemoteWebDriver driver;


    public ICView(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public RealView selectVisible() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")).click();
        return (RealView) this;
    }

    public ICXPath icxpath( ){
        return new ICXPath (driver);
    }

    public RealView sleep(double seconds) {
        try {
            Thread.sleep( (int)(seconds*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (RealView) this;
    }

    public WebElement locate(ICXPath icxpath){
        return driver.findElement(By.xpath(icxpath.get()));
    }

}
