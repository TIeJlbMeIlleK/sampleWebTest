package ru.iitdgroup.tests.webdriver.alerts;

import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class AlertRecord extends AbstractEdit<AlertRecord> {

    public AlertRecord(RemoteWebDriver driver) {
        super(driver);
    }

    public AlertRecord action(String action) {
        driver.findElementByXPath("//*[text()='Actions']").click();
        driver.findElementByXPath(String.format("//div[@class='qtip-content']//following::a[text()='%s']", action)).click();
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(3);
        return getSelf();
    }

    public AlertRecord rdak() {
        driver.findElementByLinkText("РДАК").click();
        sleep(3);
        return getSelf();
    }

    /**
     * В РДАК "Не мой платеж"
     */
    public AlertRecord notMyPayment() {
        driver.findElementByXPath("//button[@id='_ic_rdak_btn_cancel']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Клиент не подтвержден"
     */
    public AlertRecord clientNotConfirmed() {
        driver.findElementByXPath("//button[@id='_ic_rdak_notActualPhone']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Клиент перезвонит сам"
     */
    public AlertRecord theClientWillCallHimself() {
        driver.findElementByXPath("//button[@id='_ic_rdak_btn_client_call']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Перезвонить позже"
     */
    public AlertRecord callBackLater() {
        driver.findElementByXPath("//button[@id='_ic_rdak_btn_call_later']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Не известно"
     */
    public AlertRecord NotKnown() {
        driver.findElementByXPath("//button[@id='_ic_rdak_btn_unknown']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Мой платеж"
     */
    public AlertRecord MyPayment() {
        driver.findElementByXPath("//button[@id='_ic_rdak_btn_ok']").click();
        sleep(2);
        return getSelf();
    }

    /**
     * В РДАК "Не дозвонился"
     */
    public AlertRecord unSuccessfulCall() {
        driver.findElementByXPath("//button[@id='_ic_rdak_unSuccessfulCall']").click();
        sleep(2);
        return getSelf();
    }

    @Override
    public AlertRecord getSelf() {
        return this;
    }

    public AlertRecord goToTransactionPage() {
        driver.findElementByXPath("//div[@id='_panel_0_0_:content']//table[@class='DetailsLayoutPanel ']/tbody/tr[3]/td[2]//a").click();
        return getSelf();
    }

    public String getLastDate() {
        return driver.findElementByXPath("//tbody[@id='j_id226:0:j_id229:tbody_element']/tr[1]/td[4]/table/tbody/tr/td/table/tbody/tr/td[2]").getText();
    }
}
