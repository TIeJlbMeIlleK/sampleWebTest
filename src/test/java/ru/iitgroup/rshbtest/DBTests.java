package ru.iitgroup.rshbtest;


import org.testng.annotations.Test;
import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.webdriver.referencetable.Table;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class DBTests extends RSHBTests {


    @Test
    public void checkDBData() throws Exception {
        final String[][] rows;
        try (Database db = new Database(props)) {
            rows = db.select()
                    .field("id")
                    .field("NAME")
                    .from("DBOCHANNEL")
                    .with("id", "=", "1")
                    .with("id", "=", "2")
                    .setFormula("1 OR 2")
                    .get();
            assertEquals("1", rows[0][0]);
            assertEquals("2", rows[1][0]);
//            for (String[] row : rows) {
//                System.out.println(String.join("\t", row));
//            }
        }
    }
}
