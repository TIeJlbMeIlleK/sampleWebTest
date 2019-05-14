package ru.iitgroup.tests.webdriver;

import javax.xml.xpath.XPath;

public class ICXPath {
    public static final String ANYWHERE = ".//*";
    protected StringBuilder sb = new StringBuilder();

    public static String normalized(String value) {
        return "[normalize-space(text()) and normalize-space(.)='" + value + "']";
    }

    // (.//*[normalize-space(text()) and normalize-space(.)='Name'])[2]/preceding::input[1]")
    public ICXPath element(String heading, int nth) {
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

    public ICXPath preceding(WebElements element, int nth) {
        sb
                .append("/preceding::")
                .append(element.name)
                .append("[")
                .append(nth)
                .append("]");
        return this;
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

    public ICXPath specific(String xpathText){
        sb.append( xpathText);
        return this;
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

    @Override
    public String toString() {
        return get();
    }
}
