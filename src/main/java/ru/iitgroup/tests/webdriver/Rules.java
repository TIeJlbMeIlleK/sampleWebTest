package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class Rules extends ICView<Rules> {


    public Rules(RemoteWebDriver driver) {
        super(driver);
    }

    public Rules selectRule(String heading) {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='" + heading + "'])[1]/preceding::input[2]")).click();
        return this;
    }

    public Rules activate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
//        driver.element(By.xpath("//*[@class=\"qtip-content\"]//*[text() ='Activate']")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Activate']")).click();
        return this;
    }

    public Rules deactivate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Deactivate']")).click();
        return this;
    }


    public enum Actions {
        CHANGE_WORKSPACE(1),
        DELETE(2),
        ACTIVATE(3),
        DEACTIVATE(4);

        private final int pos;
        Actions(int pos) {
            this.pos = pos;
        }


    }
}