package ru.iitdgroup.tests.webdriver.scoringmodels;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.referencetable.Record;
import ru.iitdgroup.tests.webdriver.referencetable.TableEdit;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleSpoiler;

public class ScoringModelRecord extends AbstractEdit<ScoringModelRecord> {

    public ScoringModelRecord(RemoteWebDriver driver) {
        super(driver);
    }

    /**
     * Включает редактирование основных полей (щёлкает на кнопку edit)
     * @return
     */
    public ScoringModelRecord edit() {
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        waitUntil("//a[@id='btnSave']");
        return getSelf();
    }

    public ScoringModelRecord save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
        element.click();
        waitUntil("//a[@id='btnEdit']");
        return getSelf();
    }

    public RuleSpoiler<ScoringModelRecord> openRule(String name) {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::*[text()='%s']", name))
                .click();
        sleep(1);
        return new RuleSpoiler<>(driver, name, getSelf());
    }

    @Override
    public ScoringModelRecord getSelf() {
        return this;
    }
}
