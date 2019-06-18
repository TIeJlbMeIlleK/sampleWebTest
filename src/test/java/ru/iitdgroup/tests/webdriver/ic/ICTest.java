package ru.iitdgroup.tests.webdriver.ic;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.properties.TestProperties;

import java.io.FileInputStream;

public class ICTest {

    @Test
    public void testTakeScreenshot() throws Exception {
        TestProperties props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
        IC ic = new IC(props);
        try {

            System.out.println(ic.takeScreenshot("Раз / path").toString());
        } finally {
            ic.close();
        }
    }
}