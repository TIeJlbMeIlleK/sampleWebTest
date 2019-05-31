package ru.iitgroup.tests.webdriver.importruletable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.Table;
import ru.iitgroup.tests.webdriver.ic.AbstractViewContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контекст для работы с экранной формой импорта правил.
 */
public class Context extends AbstractViewContext<Context> {

    private final static Path RULES_DATA_BASE_DIR = Paths.get("resources/ruletables");
    private String fileName;

    public Context(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected Context getSelf() {
        return this;
    }

    public Context chooseTable(Table table) {
        String idOfRadioButton = driver.findElementByXPath(
                String.format("//table//label[text()='%s']", table.getTableName())
        ).getAttribute("for");

        driver.findElementByXPath(
                String.format("//table//input[@id='%s']", idOfRadioButton)
        ).click();

        return this;
    }

    public Context chooseFile(String fileName) {
        this.fileName = fileName;

        String absolutePath = RULES_DATA_BASE_DIR.resolve(Paths.get(fileName))
                .toAbsolutePath()
                .toString();
        driver.findElementByXPath("//table//input[@type='file']").sendKeys(absolutePath);

        return this;
    }

    public Context load() {
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

    public Context rollback() {
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
                    "Произошла ошибка, во время возврата импортированных правил, " +
                            "связанная с взаимодействием с экранной формой.",
                    ex
            );
        }

        return this;
    }
}
