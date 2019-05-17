package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTable extends ICView{



    public ReferenceTable(RemoteWebDriver driver) {
        super( driver);

    }

    public ReferenceTableEdit addRecord() {
        icxpath()
                .element("Actions")
                .preceding(ICXPath.WebElements.IMG)
                .click();
        return new ReferenceTableEdit(driver);
    }

    public ReferenceTableRecord selectRecord(String... rowValues) {
        icxpath()
                .row(rowValues)
                .click();
        return new ReferenceTableRecord(driver);
    }

}
