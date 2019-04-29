package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;

public class IC {

    private final String CLOSE_ACTION = "Close";
    RemoteWebDriver driver;

    public IC(RemoteWebDriver driver) {
        this.driver = driver;
    }


    public void close() {
        try {
            //FIXME: не работает logoff
            //driver.findElement(By.linkText("Logoff")).click();
        } finally {
            driver.quit();
        }
    }

    public void locateView(TopMenu item) {
        driver.findElement(By.linkText(item.heading)).click();
    }

    public ReferenceTable locateTable(AllTables table) {
        locateView(TopMenu.REFERENCE_DATA);
        driver.findElement(By.linkText(table.heading)).click();
        return new ReferenceTable(driver);
    }

    public enum TopMenu {
        REFERENCE_DATA("Reference Data");

        public final String heading;

        TopMenu(String heading) {
            this.heading = heading;
        }
    }

    public enum AllTables {
        VIP_БИК_СЧЁТ("(Rule_tables) VIP клиенты БИКСЧЕТ");


        public final String heading;

        AllTables(String heading) {

            this.heading = heading;
        }
    }
}
