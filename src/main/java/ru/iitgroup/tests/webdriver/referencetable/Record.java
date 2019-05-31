package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractEdit;

public class Record extends AbstractEdit<Record> {

    public Record(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected Record getSelf() {
        return this;
    }

    public TableEdit edit() {
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
        sleep(0.5);
        return new TableEdit(driver);
    }
}
