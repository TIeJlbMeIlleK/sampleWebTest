package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractEdit;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Контекст для редактирования правил.
 */
public class RuleEdit extends AbstractEdit<RuleEdit> {

    public RuleEdit(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected RuleEdit getSelf() {
        return this;
    }

    public Rules save() {
        throw new NotImplementedException();

//        return new RuleContext(driver);
    }
}
