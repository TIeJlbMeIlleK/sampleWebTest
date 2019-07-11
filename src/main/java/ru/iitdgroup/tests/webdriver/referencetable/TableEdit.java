package ru.iitdgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

public class TableEdit extends AbstractEdit<TableEdit> implements TabledView {

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

    public TableEdit fillUser(String fieldName, String dboId) {
        setTableFilter(fieldName, "Equals", dboId);
        refreshTable();

        driver.findElementByXPath("//a[text()='Select']").click();

        waitUntil("//a[@id='btnSave']");

        return this;
    }

    public Record save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        waitUntil("//a[@id='btnEdit']");
        return new Record(driver);
    }
}
