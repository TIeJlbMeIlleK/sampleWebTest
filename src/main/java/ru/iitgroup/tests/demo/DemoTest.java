package ru.iitgroup.tests.demo;

import ru.iitgroup.tests.webdriver.IC;
import ru.iitgroup.tests.webdriver.ICFactory;

import static ru.iitgroup.tests.webdriver.IC.AllTables.VIP_БИК_СЧЁТ;

public class DemoTest {
    public static void main(String[] args) {
        System.out.println("Starting...");

        IC ic = ICFactory.open(
                "http://192.168.7.151:7780/InvestigationCenter/",
                "ic_admin", "ic_admin");

        ic.locateTable(VIP_БИК_СЧЁТ)
                .addRecord()
                .fillMasked("Бик банка VIP:", "123456789")
                .save();

        ic.close();
        System.out.println("Finished Ok.");
    }
}
