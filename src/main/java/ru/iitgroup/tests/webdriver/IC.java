package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Calendar;

public class IC {

    private final String CLOSE_ACTION = "Close";
    RemoteWebDriver driver;

    public IC(RemoteWebDriver driver) {
        this.driver = driver;
    }

    void takeScreenshot() throws IOException {
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);

        final String date = LocalDateTime.now().toString()
                .replace("T","_")
                .replace(":","-");

        Files.copy(scrFile.toPath(), Paths.get(String.format("c:\\tmp\\IC%s.png", date)));
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
