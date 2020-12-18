package ru.iitdgroup.tests.webdriver.rabbit;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Rabbit implements AutoCloseable {

    private final String CLOSE_ACTION = "Close";
    private final TestProperties props;

    private final RemoteWebDriver driver;
    private final View view;

    private final static Path RESOURCES = Paths.get("resources");
    private static final String DEFAULT_VES_PATH = "/ves/ves-data.json";
    private static final String DEFAULT_KAF_ALERT_PATH = "/caf/caf_alert.json";
    private static final String DEFAULT_KAF_NOT_TRANS_PATH = "/caf/caf_notFinance.json";
    private String vesResponse;
    private String cafAlertResponse;
    private String cafNonFinanceResponse;


    public Rabbit(TestProperties props) {
        withVesResponse(DEFAULT_VES_PATH);
        withCafAlertResponse(DEFAULT_KAF_ALERT_PATH);
        withCafNotFinanceResponse(DEFAULT_KAF_NOT_TRANS_PATH);
        this.props = props;
        //TODO: перенести путь в файл настроек - оно системно-специфическое
        System.setProperty("webdriver.chrome.driver", props.getChromeDriverPath());

        if (props.getChromeHeadlessMode()) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            //options.addArguments("--ignore-certificate-errors");
            options.addArguments("--disable-extensions");
            driver = new ChromeDriver(options);
        } else {
            driver = new ChromeDriver();
            driver.manage().window().setSize(new Dimension(2000, 1600));
            driver.manage().window().setPosition(new Point(0, 0));
        }

        driver.manage().timeouts().implicitlyWait(7, TimeUnit.SECONDS);
        try {
            driver.get(props.getRabbitUrl());
            driver.findElement(By.name("username")).clear();
            driver.findElement(By.name("username")).sendKeys(props.getRabbitUser());
            driver.findElement(By.name("password")).clear();
            driver.findElement(By.name("password")).sendKeys(props.getRabbitPassword());
            driver.findElement(By.xpath("//*[@id=\"login\"]/form/table/tbody/tr[3]/td/input")).click();
            driver.get(props.getRabbitUrl());
            view = new View(driver);
        } catch (Exception e) {
            driver.close();
            throw e;
        }
    }

    public Rabbit getAllQueues(){
        driver.findElementByXPath("//*[@id=\"tabs\"]/li[5]/a").click();
        return this;
    }

    public Rabbit getQueue(String fieldname){
        driver.findElementByXPath("//a[text()='"+fieldname+"']").click();
        return this;
    }

    public Rabbit sendMessage(){
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/h2").click();
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/div/form/table/tbody/tr[5]/td/textarea").click();
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/div/form/table/tbody/tr[5]/td/textarea").clear();
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/div/form/table/tbody/tr[5]/td/textarea").sendKeys(getVesResponse());
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/div/form/input[4]").click();
        driver.findElementByXPath("//span[text()='Close']").click();
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/div/form/table/tbody/tr[5]/td/textarea").clear();
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/h2").click();
        return this;
    }

    public Rabbit closePublishMessage(){
        driver.findElementByXPath("//*[@id=\"main\"]/div[4]/h2").click();
        return this;
    }

    public Rabbit withVesResponse(String vesResponseFile) {
        try {
            this.vesResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + vesResponseFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Rabbit withCafAlertResponse(String cafAlertFile) {
        try {
            this.cafAlertResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + cafAlertFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Rabbit withCafNotFinanceResponse(String cafNotFinanceFile) {
        try {
            this.cafNonFinanceResponse = Files
                    .lines(Paths.get(RESOURCES.toAbsolutePath() + "/" + cafNotFinanceFile), StandardCharsets.UTF_8)
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }


    public void close() {
        try {
            //FIXME: не работает logoff
            //driver.element(By.linkText("Logoff")).click();
        } finally {
            driver.quit();
        }
    }

    public String getVesResponse() {
        return vesResponse;
    }

    public String setVesResponse(String vesResponse) {
        this.vesResponse = vesResponse;
        return vesResponse;
    }

    @Override
    protected void finalize() throws Throwable {
        if (driver != null) driver.close();
        super.finalize();

    }

    /**
     * Возврат на основной экран
     *
     * @return
     */


    class View extends AbstractView<View> {
        public View(RemoteWebDriver driver) {
            super(driver);
        }

        @Override
        public View getSelf() {
            return this;
        }
    }

    public RemoteWebDriver getDriver() {
        return driver;
    }
}
