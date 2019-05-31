package ru.iitgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractEditContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Контекст для редактирования правил.
 */
public class EditContext extends AbstractEditContext<EditContext> {

    public EditContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected EditContext getSelf() {
        return this;
    }

    public Context save() {
        throw new NotImplementedException();

//        return new RuleContext(driver);
    }
}
