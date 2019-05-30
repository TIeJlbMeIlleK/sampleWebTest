package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractICEditorContext;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

//TODO: унаследовать от ICEdit
public class ReferenceTableEditContext
        extends AbstractICEditorContext<ReferenceTableEditContext> {

    private static final String SAVE_ACTION = "Save";

    public ReferenceTableEditContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected ReferenceTableEditContext getSelf() {
        return this;
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    //TODO: вынести в ICEdit extends ICView
    public ReferenceTableEditContext fillMasked(AllFields field, String fieldText) {
        return fillMasked(field.heading, fieldText);
    }

    public ReferenceTableEditContext fillMasked(String fieldName, String fieldText) {
        icxpath()
                .element(fieldName)
                .following(ICXPath.WebElements.INPUT)
                .type(fieldText);
        return this;
    }

    public ReferenceTableRecordContext save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        return new ReferenceTableRecordContext(driver);
    }
}
