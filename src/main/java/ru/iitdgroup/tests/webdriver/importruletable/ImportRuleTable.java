package ru.iitdgroup.tests.webdriver.importruletable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.fail;

/**
 * Контекст для работы с экранной формой импорта правил.
 */
public class ImportRuleTable extends AbstractView<ImportRuleTable> {


    private String fileName;

    public ImportRuleTable(String tableHeading, RemoteWebDriver driver) {
        super(driver);
        chooseTable(tableHeading);
    }

    public ImportRuleTable chooseTable(String tableHeading) {
        driver.findElementByXPath(String.format("//label[text()='%s']/preceding::input[1]", tableHeading)
        ).click();

        return this;
    }

    public ImportRuleTable chooseFile(String fileName) {
        this.fileName = fileName;

        String absolutePath = Paths.get("resources/ruletables").resolve(Paths.get(fileName))
                .toAbsolutePath()
                .toString();
        driver.findElementByXPath("//table//input[@type='file']").sendKeys(absolutePath);

        return this;
    }

    public ImportRuleTable load() {
        driver.findElementByXPath("//button[contains(.,'Загрузить')]").click();
        List<WebElement> errors = driver.findElementsByXPath("//li[@class='globalMessagesError']");

        if (errors.size() > 0) {
            String errorsDescription = errors.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.joining(", "));

            fail(
                    String.format(
                            "Произошла ошибка во время импорта правил из файла %s. Описание ошибки: %s",
                            this.fileName,
                            errorsDescription
                    )
            );
        }

        return this;
    }

    public ImportRuleTable rollback() {
        driver.findElementByXPath("//button[contains(.,'Возврат')]").click();

        try {
            driver.findElementsByXPath("//button[contains(.,'Yes')]").stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElseThrow(Error::new)
                    .click();

            List<WebElement> errors = driver.findElementsByXPath("//li[@class='globalMessagesError']");

            if (errors.size() > 0) {
                String errorsDescription = errors.stream()
                        .map(WebElement::getText)
                        .collect(Collectors.joining(", "));

                fail(String.format(
                        "Во время отката rule_tables возникли ошибки: %s",
                        errorsDescription
                ));
            }
        } catch (Error ex) {
            fail(
                    "Произошла ошибка, во время отката импорта rule_tables связанная с взаимодействием с экранной формой",
                    ex
            );
        }

        return this;
    }

    @Override
    protected ImportRuleTable getSelf() {
        return this;
    }
}
