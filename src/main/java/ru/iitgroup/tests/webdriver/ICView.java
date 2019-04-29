package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.nio.file.Files;

public abstract class ICView {
    protected final RemoteWebDriver driver;

    public ICView(RemoteWebDriver driver) {
        this.driver = driver;
    }



}
