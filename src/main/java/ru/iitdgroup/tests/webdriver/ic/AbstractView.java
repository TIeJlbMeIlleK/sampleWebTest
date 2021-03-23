package ru.iitdgroup.tests.webdriver.ic;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Абстрактный контекст для работы с экранными формами.
 *
 * @param <S> Класс-наследник этой абстракции
 */
public abstract class AbstractView<S> {

    protected final RemoteWebDriver driver;
    protected final WebDriverWait wait;

    public AbstractView(RemoteWebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, 5);
    }

    public S selectVisible() {
        //driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")).click();
        icxpath()
                .element("Name", 2)
                .preceding(ICXPath.WebElements.INPUT)
                .click();
        return getSelf();
    }

    public ICXPath icxpath() {
        return new ICXPath(driver);
    }

    public S sleep(double seconds) {
        try {
            Thread.sleep((int) (seconds * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return getSelf();
    }

    public S sleep(String xpath) {
        waitUntil(xpath);
        return getSelf();
    }

    public WebElement waitUntil(String xPath) {
        return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
    }

    public WebElement locate(ICXPath icxpath) {
        return driver.findElement(By.xpath(icxpath.get()));
    }

    /**
     * Метод-заглушка, который предназначен для возврата конкретного типа объекта функциями его родителя.
     * Должен быть перекрыт в каждом классе потомке
     * @return S экземпляр конкретного класса
     */
    public abstract S getSelf();

    public RemoteWebDriver getDriver() {
        return driver;
    }


    /**
     * Отметить элемент красной рамкой, выполнить scroll к нему
     * @param xpath путь
     * @return найденный элемент
     */
    public WebElement mark(String xpath) {
        WebElement elem = driver.findElementByXPath(xpath);
        return mark( elem);
    }

    /**
     * Отметить элемент красной рамкой, выполнить scroll к нему
     * @param elem элемент для обработки
     * @return входной параметр
     */
    public WebElement mark(WebElement elem) {
        Actions actions = new Actions(driver);
        actions.moveToElement(elem);
        actions.perform();
        ((JavascriptExecutor)driver).executeScript("arguments[0].style.border='3px solid red'", elem);
        return elem;
    }

    public AbstractView<S> getActions(){
        getDriver()
                .findElementByXPath("//*[@id=\"moduleDetailsActionsToolbar\"]/div/table/tbody/tr/td[5]/table/tbody/tr/td/span/img").click();
        return this;
    }

    public AbstractView<S> getActionsForClient(){
        getDriver()
                .findElementByXPath("//span[text()='Actions']").click();
        return this;
    }

    public AbstractView<S> doAction(String text) {
        getDriver()
                .findElementByXPath("//*[@id=\"qtip-0-content\"]/a[text()='"+text+"']")
                .click();
        return this;
    }

    public AbstractView<S> approved() {
        getDriver()
                .findElementByXPath("/html/body/div[16]/div[3]/div/button[2]/span")
                .click();
        sleep(2);
        return this;
    }

    public AbstractView<S>  refreshTab() {
        getDriver()
                .findElementByXPath("//img[@title='Refresh']")
                .click();
        return this;
    }



}
