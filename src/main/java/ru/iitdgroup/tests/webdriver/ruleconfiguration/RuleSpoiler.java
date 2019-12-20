package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class RuleSpoiler<P extends AbstractView> extends AbstractEdit<RuleSpoiler> {

    private final String name;
    private final P parent;

    public RuleSpoiler(RemoteWebDriver driver, String name, P parent) {
        super(driver);
        this.name = name;
        this.parent = parent;
    }

    public RuleSpoiler<P> edit() {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::*[text()='%s']//following::img[contains(@class, 'newEditRuleMain')]", name))
                .click();
        sleep(2);
        return getSelf();
    }

    public RuleSpoilerBlock editBlock(int position) {
        driver.executeScript("window.scrollTo(0,10000)");
        return new RuleSpoilerBlock<>(driver, position, getSelf());
    }

    public RuleSpoiler<P> save() {
        driver.findElementByXPath(String.format("//*[@id='contentBody']//following::*[text()='%s']//following::*[text()='Save']", name))
                .click();
        sleep(5);
        return getSelf();
    }

    @Override
    public RuleSpoiler<P> getSelf() {
        return this;
    }

    public P getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }
}
