package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.remote.RemoteWebDriver;

public class ReferenceTable<T extends ICView> extends ICView<T>{



    public ReferenceTable(RemoteWebDriver driver) {
        super( driver);

    }

    public ReferenceTableEdit addRecord() {
        icxpath()
                .element("Actions",1)
                .preceding(ICXPath.WebElements.IMG,1)
                .click();
        return new ReferenceTableEdit(driver);
    }

    public ReferenceTableRecord<T> selectRecord(String... rowValues) {
        icxpath()
                .row("123456789","123456789123")
                .click();
        return new ReferenceTableRecord<>(driver);
    }

}
