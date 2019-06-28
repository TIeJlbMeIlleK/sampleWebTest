package ru.iitdgroup.rshbtest;


import org.testng.annotations.Test;
import ru.iitdgroup.tests.dbdriver.Database;

import static org.testng.Assert.assertEquals;

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
                    .setFormula("OR")
                    .get();
            assertEquals("1", rows[0][0]);
            assertEquals("2", rows[1][0]);
//            for (String[] row : rows) {
//                System.out.println(String.join("\t", row));
//            }
        }
    }
}
