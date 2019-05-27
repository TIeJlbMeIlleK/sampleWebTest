package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.ICView;

public class Rules extends ICView<Rules> {

    public Rules(RemoteWebDriver driver) {
        super(driver);
    }

    public Rules selectRule(String heading) {
        //language=XPath
        final String xpath = ".//*[normalize-space(text())='" + heading + "'][1]/preceding::input[2][@type='checkbox']";

        final WebElement ruleCheckBox = driver.findElement(By.xpath(".//*[text()='R01_ExR_04_InfectedDevice'][1]/preceding::input[2][@type='checkbox']"));


//
//        WebElement ruleCheckBox = icxpath()
//                .element(heading)
//                .preceding(ICXPath.WebElements.INPUT,2)
//                .specify("@type='checkbox'")
//                .locate();
        ruleCheckBox.click();
        while ( !ruleCheckBox.isSelected()){
            System.out.println("waiting 1ms");
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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