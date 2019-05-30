package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractICEditorContext;

public class ReferenceTableRecordContext extends AbstractICEditorContext<ReferenceTableRecordContext> {

    public ReferenceTableRecordContext(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected ReferenceTableRecordContext getSelf() {
        return this;
    }

    public ReferenceTableEditContext edit(){
        driver.findElement(By.xpath("//a[@id='btnEdit']/img")).click();
        //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
        sleep(0.5);
        return new ReferenceTableEditContext(driver);
    }
}
