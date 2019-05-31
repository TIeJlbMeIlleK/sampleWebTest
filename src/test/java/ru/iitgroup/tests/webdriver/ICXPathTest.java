package ru.iitgroup.tests.webdriver;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

import static org.testng.Assert.assertEquals;
import static ru.iitgroup.tests.webdriver.ic.ICXPath.WebElements.IMG;
import static ru.iitgroup.tests.webdriver.ic.ICXPath.WebElements.INPUT;

public class ICXPathTest {

    ICXPath icxp;

    @BeforeMethod
    public void setUp() {
        icxp = new ICXPath(null);
    }

    @Test
    public void testPreceding() {
        //language=XPath
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/preceding::img[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .preceding(IMG, 1)
                .get();
        assertEquals(xpath, expected);
    }


    @Test
    public void testFollowing() {
        //language=XPath
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/following::input[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .following(INPUT, 1)
                .get();
        assertEquals(xpath, expected);

    }

    @Test
    public void testSubelement() {
        //language=XPath
        final String expected = "(.//*[normalize-space(text()) and normalize-space(.)='Actions'])[1]/img[1]";
        final String xpath = icxp
                .element("Actions", 1)
                .next(IMG, 1)
                .get();
        assertEquals(xpath, expected);
    }

    @Test
    public void testRow() {
        //language=XPath
        final String expected = ".//*[contains(text(),'123456789') and contains(text(),'123456789123')]";

        final String xpath = icxp
                .row("123456789", "123456789123")
                .get();
        assertEquals(xpath, expected);
    }

    @Test
    public void testSpecify() {
        //language=XPath
        //FIXME: Потенциально работает и более простая форма. Но пока оставм сложную.
        //final String expected = "//*[text()='R01_ExR_04_InfectedDevice'][1]/preceding::input[2][@type='checkbox']";
        //language=XPath
        final String expected = "((.//*[normalize-space(text()) and normalize-space(.)='R01_ExR_04_InfectedDevice'])[1]/preceding::input[2])[@type='checkbox']";
        final String xpath = icxp
                .element("R01_ExR_04_InfectedDevice")
                .preceding(INPUT, 2)
                .specify("@type='checkbox'")
                .get();
        assertEquals(xpath, expected);
    }

}