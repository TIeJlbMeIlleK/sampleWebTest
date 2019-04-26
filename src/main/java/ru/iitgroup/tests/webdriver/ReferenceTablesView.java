package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;

import static ru.iitgroup.tests.webdriver.InvestigationCenter.driver;

public class ReferenceTablesView extends ICView {
    public static void locateTable(String tableName) {
        driver.findElement(By.linkText(tableName)).click();
    }
}
