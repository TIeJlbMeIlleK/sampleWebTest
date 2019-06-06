package ru.iitgroup.tests.webdriver.importruletable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractView;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контекст для работы с экранной формой импорта правил.
 */
public class ImportRuleTable extends AbstractView<ImportRuleTable> {

    private String fileName;

    public ImportRuleTable(RemoteWebDriver driver) {
        super(driver);
    }

    public ImportRuleTable chooseTable(String tableHeading) {
        String idOfRadioButton = driver.findElementByXPath(
                String.format("//allTables//label[text()='%s']", tableHeading)
        ).getAttribute("for");

        driver.findElementByXPath(
                String.format("//allTables//input[@id='%s']", idOfRadioButton)
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

            throw new Error(
                    String.format(
                            "Произошла ошибка, во время импорта правил. Имя файла: %s Ошибки: %s",
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

                throw new Error(String.format(
                        "Произошла ошибка, во время возврата импортированных правил. Ошибки: %s",
                        errorsDescription
                ));
            }
        } catch (Error ex) {
            throw new Error(
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
