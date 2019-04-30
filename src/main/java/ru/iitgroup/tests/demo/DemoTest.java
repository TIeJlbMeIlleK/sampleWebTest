package ru.iitgroup.tests.demo;

import ru.iitgroup.tests.webdriver.IC;
import ru.iitgroup.tests.webdriver.ICFactory;

import static ru.iitgroup.tests.webdriver.AllFields.VIP_БИК_СЧЁТ$БИК;
import static ru.iitgroup.tests.webdriver.AllFields.VIP_БИК_СЧЁТ$СЧЁТ;
import static ru.iitgroup.tests.webdriver.IC.AllTables.VIP_БИК_СЧЁТ;

public class DemoTest {
    public static void main(String[] args) {
        System.out.println("Starting...");

            IC ic = ICFactory.open(
                    "http://192.168.7.151:7780/InvestigationCenter/",
                    "ic_admin", "ic_admin");
        try {

            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    .selectRule("R01_ExR_04_InfectedDevice")
                    .activate();


            ic.close();
            System.out.println("Finished Ok.");
        } catch (Exception ex){
            System.err.println(String.format("Error: %s", ex.getMessage()));
            ic.getDriver().close();
        }
    }
}
