package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractICEditorContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Контекст для редактирования правил.
 */
public class RuleEditorContext extends AbstractICEditorContext<RuleEditorContext> {

    public RuleEditorContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected RuleEditorContext getSelf() {
        return this;
    }

    public RuleContext save() {
        throw new NotImplementedException();

//        return new RuleContext(driver);
    }
}
