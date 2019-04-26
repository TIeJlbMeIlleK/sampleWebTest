package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.fail;

public class Test1Test {
    private WebDriver driver;
    private String baseUrl;
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        System.setProperty("webdriver.chrome.driver", "c:\\webdrivers\\chromedriver.exe");
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Test
    public void test02() throws Exception {
        driver.get("http://192.168.7.151:7780/InvestigationCenter");
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("ic_admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("ic_admin");
        driver.findElement(By.linkText("LOGIN")).click();
        driver.findElement(By.linkText("Reference Data")).click();
        driver.findElement(By.linkText("(Rule_tables) VIP клиенты БИКСЧЕТ")).click();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]")).click();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Бик банка VIP:'])[1]/following::input[1]")).sendKeys("123456789");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Счет получатель VIP:'])[1]/following::input[1]")).sendKeys("12345678912345678912");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Причина занесения:'])[1]/following::input[1]")).clear();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Причина занесения:'])[1]/following::input[1]")).sendKeys("Перенос из карантина");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Comment:'])[1]/following::input[1]")).clear();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Comment:'])[1]/following::input[1]")).sendKeys("123");
        driver.findElement(By.linkText("Save")).click();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private String closeAlertAndGetItsText() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alertText;
        } finally {
            acceptNextAlert = true;
        }
    }

    private boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }
}
