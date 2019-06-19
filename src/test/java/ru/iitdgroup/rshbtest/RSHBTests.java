package ru.iitdgroup.rshbtest;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.properties.TestProperties;
import ru.iitdgroup.tests.webdriver.ic.IC;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

public abstract class RSHBTests {

    protected TestProperties props;
    protected IC ic;
    private String testName;

    @BeforeClass
    public void setUpProperties() throws IOException {
        props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
    }

    @BeforeMethod
    public void setupMethod(Method method) {
        ic = new IC(props);

        testName = Arrays.stream(method.getAnnotations())
                .map(this::getTestName)
                .findFirst()
                .orElse("");
    }


    /**
     * Метод корректного закрытия общих ресурсов, на случай, если тест упадёт
     *
     * @param result
     */
    @AfterMethod
    public void tearDownMethod(ITestResult result) {
        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                ic.takeScreenshot(testName);
            }
        } finally {
            ic.close();
        }
    }


    private String getTestName(Annotation a) {
        if (a instanceof Test) {
            //TODO: В этом месте каждый тест должен отмечаться в некотором логе
            return ((Test) a).description();
        } else {
            throw new IllegalStateException("Метод, помеченный аннотацией @Test не является тестом");
        }
    }
}
