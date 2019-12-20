package ru.iitdgroup.tests.webdriver.ruleconfiguration;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;

public class RuleSpoilerBlock<P extends RuleSpoiler> extends AbstractView<RuleSpoilerBlock> {

    private final int position;
    private final P parent;

    public RuleSpoilerBlock(RemoteWebDriver driver, int position, P parent) {
        super(driver);
        this.position = position;
        this.parent = parent;
    }

    public RuleSpoilerBlock<P> clear() {
        // TODO clear
        return getSelf();
    }

    public RuleSpoilerBlock<P> add() {
        sleep(1);
        clickByLinkText(Action.ADD.getLinkText());
        return getSelf();
    }

    public RuleSpoilerBlock<P> delete(int row) {
        sleep(1);
        selectRow(row);
        sleep(1);
        clickByLinkText(Action.DELETE.getLinkText());
        sleep(1);
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        sleep(3);
        return getSelf();
    }

    public RuleSpoilerBlock<P> up(int row) {
        sleep(1);
        selectRow(row);
        sleep(1);
        clickByLinkText(Action.UP.getLinkText());
        sleep(3);
        return getSelf();
    }

    public RuleSpoilerBlock<P> down(int row) {
        sleep(1);
        selectRow(row);
        sleep(1);
        clickByLinkText(Action.DOWN.getLinkText());
        return getSelf();
    }

    public RuleSpoilerBlock<P> top(int row) {
        sleep(1);
        selectRow(row);
        sleep(1);
        clickByLinkText(Action.TOP.getLinkText());
        sleep(3);
        return getSelf();
    }

    public RuleSpoilerBlock<P> bottom(int row) {
        sleep(1);
        selectRow(row);
        sleep(1);
        clickByLinkText(Action.BOTTOM.getLinkText());
        sleep(3);
        return getSelf();
    }

    public RuleSpoilerBlock<P> editTextField(int row, int col, String value) {
        WebElement td = getTd(row, col);
        td.click();
        sleep(1);
        WebElement input = td.findElement(By.tagName("input"));
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), value);
        input.click();
        sleep(1);
        return getSelf();
    }

    public RuleSpoilerBlock<P> select(int row, int col, String value) {
        WebElement td = getTd(row, col);
        td.click();
        sleep(1);
        Select select = new Select(td.findElement(By.tagName("select")));
        select.selectByVisibleText(value);
        sleep(3);
        return getSelf();
    }

    @Override
    public RuleSpoilerBlock<P> getSelf() {
        return this;
    }

    public P getParent() {
        return parent;
    }

    private String getBlockPath() {
        return String.format("//div[@id='pageContentBody']//following::div[text()='%s']//following::*[@class='af_table_content']", getParent().getName());
    }

    private void selectRow(int row) {
//        getBlockPath()
//                .findElement(By.className("af_table_content"))
//                .findElements(By.tagName("tr")).get(row + 1)
//                .findElement(By.tagName("input"))
//                .click();
    }

    private void clickByLinkText(String text) {
        driver.findElementsByXPath(String.format(
                "%s/../../../../../../../..//following::a[text()='%s']",
                getBlockPath(),
                text)).get(position)
                .click();
    }

    private WebElement getTd(int row, int col) {
        return driver.findElementByXPath(getBlockPath() + "[1]//following::tr[" + row + "]//following::td[" + col +"]");
    }

    public enum Action {
        ADD("Add"),
        DELETE("Delete"),
        UP("Up"),
        DOWN("Down"),
        TOP("Top"),
        BOTTOM("Bottom");

        private final String linkText;

        Action(String linkText) {
            this.linkText = linkText;
        }

        public String getLinkText() {
            return linkText;
        }
    }
}
