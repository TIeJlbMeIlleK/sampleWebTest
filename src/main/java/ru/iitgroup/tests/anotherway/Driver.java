package ru.iitgroup.tests.anotherway;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import ru.iitgroup.tests.properties.TestProperties;

import java.util.concurrent.TimeUnit;

/**
 * Утилитный класс, который поставляет драйвер для тестов
 * В этом "пути" предполагается, что все методы контекстов будут void, и они
 * будут получать один единый драйвер. Возможно, такой подход поможет упростить реализацию,
 * т.к. часть ответственность за валидное прохождение по экранным формам будет на тестировщиках,
 * которые будут писать тест. Программисту не придётся делать проверку на "дурака", чтобы ошибка падала
 * во время компиляции.
 * <p>
 * void методы, возможно, можно будет удобно связать с кукумбером. Важный момент, надо исследовать заранее.
 */
public class Driver {

    private final TestProperties props;
    private static WebDriver driver;

    public Driver(TestProperties props) {
        this.props = props;
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
            driver.close();
            e.printStackTrace();
        }
    }

    public static WebDriver getDriver() {
        return driver;
    }
}
