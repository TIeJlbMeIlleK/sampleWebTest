package ru.iitgroup.tests.webdriver.ic;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Абстрактный контекст для работы с экранными формами.
 *
 * @param <S> Класс-наследник этой абстракции
 */
public abstract class AbstractViewContext<S> {

    protected final RemoteWebDriver driver;
    protected final WebDriverWait wait;

    public AbstractViewContext(RemoteWebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, 1);
    }

    protected abstract S getSelf();

    public RemoteWebDriver getDriver() {
        return driver;
    }

    public S selectVisible() {
        //driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")).click();
        icxpath()
                .element("Name",2)
                .preceding(ICXPath.WebElements.INPUT)
                .click();
        return getSelf();
    }

    public ICXPath icxpath( ){
        return new ICXPath (driver);
    }

    public S sleep(double seconds) {
        try {
            Thread.sleep( (int)(seconds*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSelf();
    }

    public WebElement waitUntil(String xPath) { // TODO: 30.05.19 Возможно переименовать метод?
        return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
    }

    public WebElement locate(ICXPath icxpath){
        return driver.findElement(By.xpath(icxpath.get()));
    }
}
