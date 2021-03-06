package ru.iitdgroup.tests.webdriver.ic;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.administration.Workflows;
import ru.iitdgroup.tests.webdriver.alerts.Alerts;
import ru.iitdgroup.tests.webdriver.importruletable.ImportRuleTable;
import ru.iitdgroup.tests.webdriver.jobconfiguration.Jobs;
import ru.iitdgroup.tests.webdriver.referencetable.Table;
import ru.iitdgroup.tests.webdriver.report.Reports;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.Rules;
import ru.iitdgroup.tests.webdriver.scoringmodels.ScoringModels;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class IC implements AutoCloseable {

    private final String CLOSE_ACTION = "Close";
    private final TestProperties props;

    private final RemoteWebDriver driver;
    private final View view;


    public IC(TestProperties props) {
        this.props = props;
        //TODO: перенести путь в файл настроек - оно системно-специфическое
        System.setProperty("webdriver.chrome.driver", props.getChromeDriverPath());

        if (props.getChromeHeadlessMode()) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            //options.addArguments("--ignore-certificate-errors");//для быстроты игнорирует сертификаты безопасности
            options.addArguments("--disable-extensions");//отключает расширения
            driver = new ChromeDriver(options);
        } else {
            driver = new ChromeDriver();
            driver.manage().window().setSize(new Dimension(2000, 1600));
            driver.manage().window().setPosition(new Point(0, 0));
        }

        driver.manage().timeouts().implicitlyWait(7, TimeUnit.SECONDS);
        try {
            driver.get(props.getICUrl());
            driver.findElement(By.id("username")).clear();
            driver.findElement(By.id("username")).sendKeys(props.getICUser());
            driver.findElement(By.id("password")).clear();
            driver.findElement(By.id("password")).sendKeys(props.getICPassword());
            driver.findElement(By.linkText("LOGIN")).click();
            view = new View(driver);
        } catch (Exception e) {
            takeScreenshot("Open IC");
            driver.close();
            throw e;
        }
    }

    public Path takeScreenshot() {
        return takeScreenshot(null);
    }

    public Path takeScreenshot(String name) {
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String pageSource = driver.getPageSource();

        final String date = LocalDateTime.now().toString()
                .replace("T", " ")
                .replace(":", ".");

        String pictureName;
        if (name != null) {
            pictureName = name.replaceAll("\\P{L}", "_");
        } else {
            pictureName = "IC";
        }

        try {
            Path folder = Paths.get(props.getPicturesFolder());
            final Path imgPath = folder.resolve(String.format("%s at %s.png", pictureName, date));
            Files.move(scrFile.toPath(), imgPath);
            System.out.println(String.format("Изображение сохранено в файл %s", imgPath.toString()));

            final Path pageSourcePath = folder.resolve(String.format("%s at %s.htm", pictureName, date));
            Files.write(pageSourcePath, pageSource.getBytes(StandardCharsets.UTF_8));
            System.out.println(String.format("Код страницы сохранён в файл %s", pageSourcePath.toString()));
            return imgPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scrFile.toPath();
    }

    public void close() {
        try {
            //FIXME: не работает logoff
            //driver.element(By.linkText("Logoff")).click();
        } finally {
            driver.quit();
        }
    }

    public ImportRuleTable locateImportRuleTable(String tableHeading) {
        locateView(TopMenuItem.IMPORT_RULE_TABLES);
        return new ImportRuleTable(tableHeading, driver);
    }

    public Table locateTable(String tableHeading) {
        locateView(TopMenuItem.REFERENCE_DATA);
        driver.findElement(By.linkText(tableHeading)).click();
        return new Table(driver);
    }


    public Rules locateRules() {
        view.sleep(1);
        locateView(TopMenuItem.ANALYTICS);
        locateView(TopMenuItem.RULES);
        driver.findElementByXPath("//span[text()='All Rules']/..").click();
        view.sleep(2);
        driver.findElementByXPath("//td[@class='AFContentCell']//select[@class='af_selectOneChoice_content']/option[@value='4']").click();
        driver.findElementByXPath("//*[@id='toolbarActions']/div/table/tbody/tr/td[2]/a").click();
        view.sleep(1);
        driver.executeScript("window.scrollBy(0,10000)");
        view.sleep(1);
        return new Rules(driver);
    }

    public Jobs locateJobs() {
        try {
            driver.findElementByClassName("adminMenuButton").click();
        } catch (NoSuchElementException e) {
            driver.findElementByXPath("//li[@class='dropdown']//span[@class='icon-gear']").click();
        }
        view.sleep(1);
        driver.findElementByLinkText("Jobs & Services Manager").click();
        view.sleep(1);
        return new Jobs(driver);
    }

    public Workflows locateWorkflows() {
        try {
            driver.findElementByClassName("adminMenuButton").click();
        } catch (NoSuchElementException e) {
            driver.findElementByXPath("//div[@id='app-navigation'][1]//following::*[@class='icon-gear']").click();
            //driver.findElementByXPath("//ul[@class='nav navbar-nav navbar-right']/li[position()=4]").click();
        }
        view.sleep(1);
        driver.findElementByLinkText("Administration").click();
        view.sleep(1);
        driver.findElementByLinkText("Workflows").click();
        view.sleep(1);
        return new Workflows(driver);
    }

    public Workflows goToBack() {
        driver.findElementByXPath("//div[@id='app-navigation'][1]//following::*[@class='icon-gear']").click();
        driver.findElementByLinkText("Administration").click();
        return new Workflows(driver);
    }

    public Alerts locateAlerts() {
        locateView(TopMenuItem.ALERTS);
        driver.findElementByXPath("//img[@alt='Refresh']").click();
        view.sleep(3);
        return new Alerts(driver);
    }

    public Reports locateReports() {
        locateView(TopMenuItem.REPORTS);
        locateView(TopMenuItem.REPORT_MANAGEMENT);
        return new Reports(driver);
    }

    public ScoringModels locateScoringModels() {
        locateView(TopMenuItem.ANALYTICS);
        locateView(TopMenuItem.SCORING_MODELS);
        return new ScoringModels(driver);
    }

    private void locateView(TopMenuItem item) {
        driver.findElement(By.linkText(item.getHeading())).click();
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
    public IC home() {
        try {
            driver.findElementByXPath("//*[@id=\"navigateHomeLinkLogo\"]/img").click();
        } catch (NoSuchElementException e) {
            driver.findElementByClassName("icon-home").click();
        }
        return this;
    }


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
