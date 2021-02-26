package ru.iitdgroup.tests.cases.forTestNG;

import org.testng.annotations.Test;

public class MyTest {
    @Test(
            description = "Создаем клиента"
    )
    public void someTest1() {

    }

    @Test(
            description = "222",
            dependsOnMethods = "someTest1"
    )
    public void someTest2() {

    }
}
