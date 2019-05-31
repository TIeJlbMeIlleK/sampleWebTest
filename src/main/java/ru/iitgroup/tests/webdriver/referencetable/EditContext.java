package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractEditContext;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

public class EditContext extends AbstractEditContext<EditContext> {

    private static final String SAVE_ACTION = "Save";

    public EditContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected EditContext getSelf() {
        return this;
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    //TODO: вынести в ICEdit extends ICView
    public EditContext fillMasked(AllFields field, String fieldText) {
        return fillMasked(field.heading, fieldText);
    }

    public EditContext fillMasked(String fieldName, String fieldText) {
        icxpath()
                .element(fieldName)
                .following(ICXPath.WebElements.INPUT)
                .type(fieldText);
        return this;
    }

    public RecordContext save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        return new RecordContext(driver);
    }
}
