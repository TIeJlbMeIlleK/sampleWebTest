package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.RuleTemplate;
import ru.iitgroup.tests.webdriver.ic.AbstractICViewContext;

/**
 * Контекст для работы с экранной формой правил.
 */
public class RuleContext extends AbstractICViewContext<RuleContext> {

    public RuleContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected RuleContext getSelf() {
        return this;
    }

    public RuleEditorContext createRule(RuleTemplate template) {
        driver.findElementByXPath("//div[@id='toolbarActions']//td[@class='toolbarCell']//*[contains(@class,'newRule')]").click();
        waitUntil("//input[@id='ruleTemplateSearchText']");
        driver.findElementByXPath("//input[@id='ruleTemplateSearchText']").click();
        driver.findElementByXPath(String.format("//a[contains(text(),'%s')]", template.name())).click();
        waitUntil("//button[text()='OK']").click();
        return new RuleEditorContext(driver);
    }

    public RuleContext selectRule(String heading) {
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

    public RuleContext activate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
//        driver.element(By.xpath("//*[@class=\"qtip-content\"]//*[text() ='Activate']")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Activate']")).click();
        return this;
    }

    public RuleContext deactivate() {
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]")).click();
        driver.findElement(By.xpath("//div[contains(@class,\"qtip\") and contains(@aria-hidden, \"false\")]//div[@class='qtip-content']/a[text()='Deactivate']")).click();
        return this;
    }

    public enum Action {
        CHANGE_WORKSPACE(1),
        DELETE(2),
        ACTIVATE(3),
        DEACTIVATE(4);

        private final int pos;

        Action(int pos) {
            this.pos = pos;
        }
    }
}