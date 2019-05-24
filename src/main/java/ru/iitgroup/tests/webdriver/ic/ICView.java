package ru.iitgroup.tests.webdriver.ic;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

public abstract class ICView<T extends ICView> {

    protected final RemoteWebDriver driver;

    public ICView(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public T selectVisible() {
        //driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")).click();
        icxpath()
                .element("Name",2)
                .preceding(ICXPath.WebElements.INPUT)
                .click();
        return (T) this;
    }

    public ICXPath icxpath( ){
        return new ICXPath (driver);
    }

    public T sleep(double seconds) {
        try {
            Thread.sleep( (int)(seconds*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (T) this;
    }

    public WebElement locate(ICXPath icxpath){
        return driver.findElement(By.xpath(icxpath.get()));
    }
}
