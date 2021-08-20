package ru.iitdgroup.tests.properties;

import java.util.Properties;

public class TestProperties extends Properties {

    public String getICUrl() {
        return getProperty("ic.url");
    }

    public String getRabbitUrl(){
        return getProperty("rabbit.url");
    }

    public String getRabbitPassword() {
        return getProperty("rabbit.password");
    }

    public String getRabbitUser() {
        return getProperty("rabbit.user");
    }

    public String getRabbitCafClientQueueName() { return getProperty("rabbit.cafClientQueueName"); }

    public String getRabbitCafAlertQueueName() { return getProperty("rabbit.cafAlertQueueName"); }

    public String getRabbitCafFactsQueueName() { return getProperty("rabbit.cafFactsQueueName"); }

    public String getRabbitAlertFromCAFClassified() { return getProperty("rabbit.cafAlertFromCAFClassifiedQueueName"); }

    public String getICUser() {
        return getProperty("ic.user");
    }

    public String getICPassword() {
        return getProperty("ic.password");
    }

    public String getChromeDriverPath() {
        return getProperty("chromedriver.path");
    }

    public boolean getChromeHeadlessMode() {
        String value = getProperty("chromedriver.headlessMode");
        return (value.toLowerCase().equals("true"));
    }

    public String getPicturesFolder() {
        return getProperty("pictures.folder");
    }

    public String getWSUrl() { return getProperty("ws.url"); }

    public String getEsppWSUrl() { return getProperty("ws.urlEspp"); }

    public String getWSUser() { return getProperty("ws.user"); }

    public String getWSPassword() {
        return getProperty("ws.password");
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
