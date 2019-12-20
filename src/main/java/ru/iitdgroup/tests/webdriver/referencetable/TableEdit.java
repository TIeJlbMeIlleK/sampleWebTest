package ru.iitdgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.TabledView;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

public class TableEdit extends AbstractEdit<TableEdit> implements TabledView {

    private static final String SAVE_ACTION = "Save";

    public TableEdit(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    public TableEdit getSelf() {
        return this;
    }



    public TableEdit fillUser(String fieldName, String dboId) {
        icxpath()
                .element(fieldName)
                .following(ICXPath.WebElements.IMG)
                .click();
        waitUntil("//span[@title='Select Клиент']");

        // Очищаем все фильтры
        driver.findElementsByClassName("filterRemoveRow")
                .forEach(webElement -> {
                    webElement.click();
                    sleep(2);
                });

        // Добавляем новый фильтр
        driver.findElementById("base_btnReportAddFilter").click();
        sleep(2);


        Select columnField = new Select(driver.findElementsByClassName("af_selectOneChoice_content").get(0));
        columnField.selectByVisibleText("Идентификатор клиента");
        sleep(2);
        Select operatorField = new Select(driver.findElementsByClassName("af_selectOneChoice_content").get(1));
        operatorField.selectByVisibleText("Equals");

        fillInputText("Value", dboId);

        driver.findElementByXPath("//img[@title='Refresh']").click();
        sleep(2);

        driver.findElementByXPath("//a[text()='Select']").click();

        waitUntil("//a[@id='btnSave']");

        return this;
    }

    public Record save() {
        final WebElement element = driver.findElementByXPath("//a[@id='btnSave']");
       // System.out.println("============ enabled ========= "+element.isEnabled());
        element.click();
        waitUntil("//a[@id='btnEdit']");
        return new Record(driver);
    }
}
