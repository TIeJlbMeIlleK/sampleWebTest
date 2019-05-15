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
                .row("123456789","123456789123")
                .click();
        return new ReferenceTableRecord(driver);
    }

}
