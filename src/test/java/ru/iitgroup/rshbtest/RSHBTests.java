package ru.iitgroup.rshbtest;

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

    private void getTestName(Annotation a) {
        if (a instanceof Test) {
            //TODO: В этом месте каждый тест должен отмечаться в некотором логе
            System.out.println(((Test) a).testName());
        }
    }
}
