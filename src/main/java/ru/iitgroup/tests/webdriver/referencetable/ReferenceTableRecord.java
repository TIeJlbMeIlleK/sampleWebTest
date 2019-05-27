package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.ICView;

public class ReferenceTableRecord extends ICView<ReferenceTableRecord> {
    public ReferenceTableRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public ReferenceTableEdit edit(){
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
        sleep(0.5);
        return new ReferenceTableEdit(driver);
    }
}
