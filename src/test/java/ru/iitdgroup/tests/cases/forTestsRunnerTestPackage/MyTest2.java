package ru.iitdgroup.tests.cases.forTestsRunnerTestPackage;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MyTest2 {
    @Test(
            description = "Создаем клиента"
    )
    public void someTest1() {
        Assert.assertEquals(0, 1);

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "someTest1"
    )
    public void someTest2() {
        Assert.assertEquals(0, 1);

    }
}
