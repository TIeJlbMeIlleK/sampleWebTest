package ru.iitgroup.tests.webdriver.importruletable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iitgroup.tests.webdriver.Table;
import ru.iitgroup.tests.webdriver.ic.AbstractICViewContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контекст для работы с экранной формой импорта правил.
 */
public class ImportRuleTableContext extends AbstractICViewContext<ImportRuleTableContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRuleTableContext.class);

    private String fileName;

    public ImportRuleTableContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected ImportRuleTableContext getSelf() {
        return this;
    }

    public ImportRuleTableContext chooseTable(Table table) {
        String idOfRadioButton = driver.findElementByXPath(
                String.format("%s%s%s", "//table//label[text()='", table.getTableName(), "']")
        ).getAttribute("for");

        driver.findElementByXPath(
                String.format("%s%s%s", "//table//input[@id='", idOfRadioButton, "']")
        ).click();

        return this;
    }

    public ImportRuleTableContext chooseFile(String fileName) {
        this.fileName = fileName;

        String absolutePath = System.getProperty("user.dir") + "/src/test/resource/" + fileName;
        driver.findElementByXPath("//table//input[@type='file']").sendKeys(absolutePath);

        return this;
    }

    public ImportRuleTableContext load() {
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

    public ImportRuleTableContext returnLoaded() {
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
