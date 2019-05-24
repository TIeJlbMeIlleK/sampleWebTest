package ru.iitgroup.tests.webdriver.importruletable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iitgroup.tests.webdriver.ic.ICView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контекст для работы с экранной формой импорта правил.
 */
public class Context extends ICView {

    private static final Logger LOG = LoggerFactory.getLogger(Context.class);

    private String fileName;

    public Context(RemoteWebDriver driver) {
        super(driver);
    }

    public Context chooseTable(ImportRuleDictionary table) {
        String idOfRadioButton = driver.findElementByXPath(
                String.format("%s%s%s", "//table//label[text()='", table.getTableName(), "']")
        ).getAttribute("for");

        driver.findElementByXPath(
                String.format("%s%s%s", "//table//input[@id='", idOfRadioButton, "']")
        ).click();

        return this;
    }

    public Context chooseFile(String fileName) {
        this.fileName = fileName;

        String absolutePath = System.getProperty("user.dir") + "/src/test/resource/" + fileName;
        driver.findElementByXPath("//table//input[@type='file']").sendKeys(absolutePath);

        return this;
    }

    public Context load() {
        driver.findElementByXPath("//button[contains(.,'Загрузить')]").click();
        List<WebElement> errors = driver.findElementsByXPath("//li[@class='globalMessagesError']");

        if (errors.size() > 0) {
            String lineSeparator = System.getProperty("line.separator");
            String errorsDescription = errors.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.joining(", "));

            LOG.error("Произошла ошибка, во время импорта правил.{}Имя файла: {}{}Ошибки: {}",
                    lineSeparator, this.fileName, lineSeparator, errorsDescription);
            throw new ICImportRuleException();
        }

        return this;
    }

    public Context returnLoaded() {
        driver.findElementByXPath("//button[contains(.,'Возврат')]").click();

        try {
            driver.findElementsByXPath("//button[contains(.,'Yes')]").stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElseThrow(ICUnloadImportRuleException::new)
                    .click();

            List<WebElement> errors = driver.findElementsByXPath("//li[@class='globalMessagesError']");

            if (errors.size() > 0) {
                String lineSeparator = System.getProperty("line.separator");
                String errorsDescription = errors.stream()
                        .map(WebElement::getText)
                        .collect(Collectors.joining(", "));

                LOG.error("Произошла ошибка, во время возврата импортированных правил.{}Ошибки: {}",
                        lineSeparator, errorsDescription);
                throw new ICImportRuleException();
            }
        } catch (ICUnloadImportRuleException ex) {
            LOG.error("Произошла ошибка, во время возврата импортированных правил, " +
                    "связанная с взаимодействием с экранной формой.", ex);
        }

        return this;
    }
}
