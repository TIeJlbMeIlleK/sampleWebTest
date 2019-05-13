package ru.iitgroup.tests.demo;

import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.properties.TestProperties;
import ru.iitgroup.tests.webdriver.IC;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting...");
        TestProperties props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
        //enableRule(props);
        //callAntifraudWS();
        checkDBData( props);
        System.out.println("Finished Ok.");
    }

    private static void enableRule(TestProperties props) {

        IC ic = new IC( props);
        try {

            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    .selectRule("R01_ExR_04_InfectedDevice")
                    .activate();


            ic.close();
        } catch (Exception ex) {
            System.err.println(String.format("IC error: %s", ex.getMessage()));
            ic.takeScreenshot();
            ic.getDriver().close();
        }
    }


    public static void callAntifraudWS(TestProperties props) throws IOException {
        //TODO: Добавлять и удалять теги

        Random r = new Random();

        DBOAntiFraudWS ws = new DBOAntiFraudWS(props.getWSUrl());

        Transaction t = Transaction.fromFile("tran1.xml");
        t.withDBOId(2);
        t.withCIFId(1);
        t.withTransactionId(5_000_000 + r.nextInt(100000));

        ws.send(t);

        System.out.println(String.format("Response code: %d\n", ws.getResponseCode()));
        System.out.println(String.format("Code: %s, Error: %s, ErrorMessage: %s.",
                ws.getSuccessCode(),
                ws.getErrorCode(),
                ws.getErrorMessage()));
    }

    private static void checkDBData(TestProperties props) throws Exception {
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
            for (String[] row : rows) {
                System.out.println( String.join("\t",row));
            }
        }

    }

}
