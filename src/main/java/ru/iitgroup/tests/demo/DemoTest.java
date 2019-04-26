package ru.iitgroup.tests.demo;

import ru.iitgroup.tests.webdriver.InvestigationCenter;
import ru.iitgroup.tests.webdriver.ReferenceTable;
import ru.iitgroup.tests.webdriver.ReferenceTablesView;

public class DemoTest {
    public static void main(String[] args) {
        System.out.println("Starting...");
        InvestigationCenter.open("http://192.168.7.151:7780/InvestigationCenter/", "ic_admin", "ic_admin");
        InvestigationCenter.locateView("Reference Data");
        ReferenceTablesView.locateTable("(Rule_tables) VIP клиенты БИКСЧЕТ");
        ReferenceTable.addRecord();
        ReferenceTable.fillText("Бик банка VIP:", "123456789");
        ReferenceTable.save();
        InvestigationCenter.close();
        System.out.println("Finished Ok.");
    }
}
