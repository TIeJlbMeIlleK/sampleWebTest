package ru.iitdgroup.tests.webdriver.report;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

public class ReportsTest extends RSHBCaseTest {

    @Test
    public void testOpenFolder() {
        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности");
    }

    @Test
    public void testOpenRecord() {
        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов");
    }

    @Test
    public void testOpenDetail() {
        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilter("ID", "Equals", "226")
                .refreshTable()
                .openFirst();
    }

    @Override
    protected String getRuleName() {
        return null;
    }
}