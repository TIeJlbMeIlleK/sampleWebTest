package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.AllFields;
import ru.iitgroup.tests.webdriver.ic.ICView;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

public class ReferenceTableEdit extends ICView {

    private static final String SAVE_ACTION = "Save";

    public ReferenceTableEdit(RemoteWebDriver driver) {
        super(driver);
    }

    public static void deleteRecord() {
        throw new IllegalStateException("Not implemented yet");
    }

    public ReferenceTableEdit fillMasked(AllFields field, String fieldText) {
        return fillMasked(field.heading, fieldText);
    }

    public ReferenceTableEdit fillMasked(String fieldName, String fieldText) {
        icxpath()
                .element(fieldName)
                .following(ICXPath.WebElements.INPUT)
                .type(fieldText);
        return this;
    }

    public ReferenceTableRecord save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        return new ReferenceTableRecord(driver);
    }
}
