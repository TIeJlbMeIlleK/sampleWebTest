package ru.iitgroup.rshbtest;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.iitgroup.tests.properties.TestProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class RSHBTests {

    protected TestProperties props;

    @BeforeClass
    public void setUpProperties() throws IOException {
        props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
    }

    @BeforeMethod
    public void checkAnnotations(Method method) {
        for (Annotation a : method.getAnnotations()) {
            getTestName(a);
        }
    }


    /**
     * Метод корректного закрытия общих ресурсов, на случай, если тест упадёт
     * @param result
     */
    @AfterMethod
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            //TODO: Получать имя теста и делать соответствующие скриншоты из IC, потом закрывать, для чего сделать IC общим ресурсом
        }
    }


    private String getTestName(Annotation a) {
        if (a instanceof Test) {
            //TODO: В этом месте каждый тест должен отмечаться в некотором логе
            return ((Test) a).testName();
        }else {
            throw new IllegalStateException("Метод, помеченный аннотацией @Test не является тестом");
        }
    }
}
