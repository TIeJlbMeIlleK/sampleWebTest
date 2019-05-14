package ru.iitgroup.rshbtest;


import org.testng.annotations.Test;
import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.webdriver.IC;

import java.io.IOException;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class SampleTests extends RSHBTests {

    @Test
    public void enableRule() {
        IC ic = new IC(props);
        try {

            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
                    .sleep(0.5)
                    .selectRule("R01_ExR_04_InfectedDevice")
                    .sleep(0.5)
                    .activate();


            ic.close();
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            ic.getDriver().close();
            fail(message);
        }
    }

    @Test
    public void callAntifraudWS() throws IOException {
        //TODO: Добавлять и удалять теги

        Random r = new Random();

        DBOAntiFraudWS ws = new DBOAntiFraudWS(props.getWSUrl());

        Transaction t = Transaction.fromFile("tran1.xml");
        t.withDBOId(2);
        t.withCIFId(1);
        t.withTransactionId(5_000_000 + r.nextInt(100000));

        ws.send(t);

        assertEquals(200, ws.getResponseCode().intValue());

        System.out.println(String.format("Response code: %d", ws.getResponseCode()));
        System.out.println(String.format("Code: %s, Error: %s, ErrorMessage: %s",
                ws.getSuccessCode(),
                ws.getErrorCode(),
                ws.getErrorMessage()));
    }

    @Test
    private void checkDBData() throws Exception {
        final String[][] rows;
        try (Database db = new Database(props)) {
            rows = db.select()
                    .field("id")
                    .field("NAME")
                    .from("BE_BRANCH")
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
