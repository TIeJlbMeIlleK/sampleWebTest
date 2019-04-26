package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.concurrent.TimeUnit;

public class InvestigationCenter {

    private static final String CLOSE_ACTION = "Close";
    static ChromeDriver driver;

    public static void open(String url, String userName, String password) {
        System.setProperty("webdriver.chrome.driver", "c:\\webdrivers\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.get(url);
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("ic_admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("ic_admin");
        driver.findElement(By.linkText("LOGIN")).click();
    }

    public static void close() {
        try {
            //FIXME: не работает logoff
            //driver.findElement(By.linkText("Logoff")).click();
        } finally {
            driver.quit();

        }
    }

    public static void locateView(String name) {
        driver.findElement(By.linkText(name)).click();
    }

}
