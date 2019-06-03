package ru.iitgroup.tests.webdriver.ic;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Абстрактный контекст для редактирующих операций.
 *
 * @param <S> 'Self' класс-наследник этой абстракции
 */
public abstract class AbstractEdit<S> extends AbstractView<S> {

    private static final String DEFAULT_X_PATH_TEMPLATE = "//*[text()='%s']//following::%s";
    private static final String INPUT_TAG = "input";
    private static final String TEXTAREA_TAG = "textarea";

    public AbstractEdit(RemoteWebDriver driver) {
        super(driver);
    }

    public S fillTextArea(String fieldName, String input) {

        final String xpath = String.format("//*[text()='%s']//following::textarea[1]",fieldName);

        WebElement inputTextField = driver.findElementByXPath(xpath);

        //mark(inputTextField);

        inputTextField.clear();
        inputTextField.sendKeys(input);

        return getSelf();
    }

    public S fillInputText(String fieldName, String input) {
        WebElement inputTextField = driver.findElementByXPath(String.format(DEFAULT_X_PATH_TEMPLATE, fieldName, INPUT_TAG));

        inputTextField.clear();
        inputTextField.sendKeys(input);

        return getSelf();
    }

    public S fillCheckBox(String fieldName, boolean checkBoxState) {
        WebElement checkBoxField = driver.findElementByXPath(String.format(DEFAULT_X_PATH_TEMPLATE, fieldName, INPUT_TAG));

        if (checkBoxField.isSelected() != checkBoxState) {
            checkBoxField.click();
        }

        return getSelf();
    }
}
