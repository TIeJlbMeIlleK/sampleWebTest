package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ICXPath {
    public static final String ANYWHERE = ".//*";
    private final RemoteWebDriver driver;
    protected StringBuilder sb = new StringBuilder();

    public ICXPath(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public static String normalized(String value) {
        return "[normalize-space(text()) and normalize-space(.)='" + value + "']";
    }

    public ICXPath element(String heading) {
        return element(heading, 1);
    }

    public ICXPath element(String heading, int nth) {
        // (.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")
        sb
                .append("(")
                .append(ANYWHERE)
                .append(normalized(heading))
                .append(")")
                .append("[")
                .append(nth)
                .append("]")
        ;
        return this;
    }


    public ICXPath row(String... headings) {
        // .//*[contains(text(),'123456789') and  contains(text(),'123456789123')]

        final String where = Arrays.stream(headings)
                .map(h -> String.format("contains(text(),'%s')", h))
                .collect(Collectors.joining(" and "));
        sb
                .append(ANYWHERE)
                .append("[")
                .append(where)
                .append("]");
        return this;
    }

    public ICXPath preceding(WebElements element) {
        return preceding(element, 1);
    }

    public ICXPath preceding(WebElements element, int nth) {
        sb
                .append("/preceding::")
                .append(element.name)
                .append("[")
                .append(nth)
                .append("]");
        return this;
    }


    public ICXPath following(WebElements element) {
        return following(element, 1);
    }

    public ICXPath following(WebElements element, int nth) {
        sb
                .append("/following::")
                .append(element.name)
                .append("[")
                .append(nth)
                .append("]");
        return this;
    }

    public ICXPath next(WebElements element) {
        return next(element, 1);
    }

    public ICXPath next(WebElements element, int nth) {
        sb
                .append("/")
                .append(element.name)
                .append("[")
                .append(nth)
                .append("]");
        return this;
    }

    public String get() {
        return sb.toString();
    }

    public ICXPath specific(String xpathText) {
        sb.append(xpathText);
        return this;
    }

    public WebElement locate() {
        return driver.findElement(By.xpath(get()));
    }

    public WebElement click() {
        final WebElement element = locate();
        element.click();
        return element;
    }

    public WebElement type(String newText) {
        final WebElement element = locate();
        element.clear();
        element.sendKeys(newText);
        return element;
    }

    @Override
    public String toString() {
        return get();
    }


    public enum WebElements {
        INPUT("input"),
        EDIT("edit"),
        IMG("img");


        private final String name;

        WebElements(String name) {
            this.name = name;
        }

    }

}
