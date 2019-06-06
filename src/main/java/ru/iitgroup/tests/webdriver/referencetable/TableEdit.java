package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

public class TableEdit extends AbstractEdit<TableEdit> {

    private static final String SAVE_ACTION = "Save";

    public TableEdit(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected TableEdit getSelf() {
        return this;
    }

    public TableEdit fillMasked(String fieldName, String fieldText) {
        icxpath()
                .element(fieldName)
                .following(ICXPath.WebElements.INPUT)
                .type(fieldText);
        return this;
    }

    public Record save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        return new Record(driver);
    }
}
