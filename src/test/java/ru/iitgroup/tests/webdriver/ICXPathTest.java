package ru.iitgroup.tests.webdriver;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static ru.iitgroup.tests.webdriver.ICXPath.WebElements.*;

public class ICXPathTest {

    ICXPath icxp;

    @BeforeMethod
    public void setUp() {
        icxp = new ICXPath( null);
    }

    @Test
    public void testPreceding() {
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .preceding(IMG, 1)
                .get();
        assertEquals(xpath, expected);
    }


    @Test
    public void testFollowing() {
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/following::input[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .following(INPUT, 1)
                .get();
        assertEquals(xpath, expected);

    }

    @Test
    public void testSubelement() {
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .next(IMG, 1)
                .get();
        assertEquals(xpath, expected);
    }

    @Test
    public void testRow() {
        final String expected = ".//*[contains(text(),'123456789') and contains(text(),'123456789123')]";

        final String xpath = icxp
                .row("123456789","123456789123")
                .get();
        assertEquals(xpath, expected);
    }


}