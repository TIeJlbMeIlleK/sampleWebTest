package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.properties.TestProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class IC implements AutoCloseable {

    private final String CLOSE_ACTION = "Close";
    RemoteWebDriver driver;


    public IC(TestProperties props) {
        //TODO: перенести путь в файл настроек - оно системно-специфическое
        System.setProperty("webdriver.chrome.driver", props.getChromeDriverPath());
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(7, TimeUnit.SECONDS);
        try {
            driver.get(props.getICUrl());
            driver.findElement(By.id("username")).clear();
            driver.findElement(By.id("username")).sendKeys(props.getICUser());
            driver.findElement(By.id("password")).clear();
            driver.findElement(By.id("password")).sendKeys(props.getICPassword());
            driver.findElement(By.linkText("LOGIN")).click();
        } catch (Exception e) {
            takeScreenshot("Open IC");
            driver.close();
            e.printStackTrace();
        }
    }

    public void takeScreenshot() {
        takeScreenshot(null);
    }

    public void takeScreenshot(String name){
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);


        final String date = LocalDateTime.now().toString()
                .replace("T", " ")
                .replace(":", ".");

        String pictureName = name == null ? "IC" : name;

        //TODO: перенести путь в файл настроек - оно системно-специфическое
        try {
            Files.copy(scrFile.toPath(), Paths.get(String.format("c:\\tmp\\%s at %s.png", pictureName, date)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            //FIXME: не работает logoff
            //driver.element(By.linkText("Logoff")).click();
        } finally {
            driver.quit();
        }
    }

    public ReferenceTable locateTable(AllTables table) {
        locateView(TopMenu.REFERENCE_DATA);
        driver.findElement(By.linkText(table.heading)).click();
        return new ReferenceTable(driver);
    }

    public Rules locateRules() {
        locateView(TopMenu.ANALYTICS);
        locateView(TopMenu.RULES);
        return new Rules(driver);
    }

    private void locateView(TopMenu item) {
        driver.findElement(By.linkText(item.heading)).click();
    }

    public enum TopMenu {
        REFERENCE_DATA("Reference Data"),
        ANALYTICS("Analytics"),
        RULES("Rules");
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

    public RemoteWebDriver getDriver() {
        return driver;
    }
}
