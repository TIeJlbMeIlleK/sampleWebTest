package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.concurrent.TimeUnit;

public class ICFactory {
    public static IC open(String url, String userName, String password) {
        System.setProperty("webdriver.chrome.driver", "c:\\webdrivers\\chromedriver.exe");
        ChromeDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(7, TimeUnit.SECONDS);
        driver.get(url);
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("ic_admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("ic_admin");
        driver.findElement(By.linkText("LOGIN")).click();
        return new IC(driver);
    }

}
