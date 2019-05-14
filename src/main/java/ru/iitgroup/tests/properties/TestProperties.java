package ru.iitgroup.tests.properties;

import java.util.Properties;

public class TestProperties extends Properties {


    public String getICUrl() {
        return getProperty("ic.url");
    }

    public String getICUser() {
        return getProperty("ic.user");
    }

    public String getICPassword() {
        return getProperty("ic.password");
    }

    public String getChromeDriverPath() {
        return getProperty("chromedriver.path");
    }

    public String getPicturesFolder() {
        return getProperty("pictures.folder");
    }


    public String getWSUrl() {
        return getProperty("ws.url");
    }


    public String getDbDriver() {
        return getProperty("db.driver");
    }

    public String getDbUrl() {
        return getProperty("db.url");
    }

    public String getDbUser() {
        return getProperty("db.user");
    }

    public String getDbPassword() {
        return getProperty("db.password");
    }

}