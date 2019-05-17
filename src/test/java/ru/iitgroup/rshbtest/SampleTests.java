package ru.iitgroup.rshbtest;


import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;
import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.webdriver.AllFields;
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
                    .activate()
                    .sleep(3)
            ;
        } catch (Exception ex) {
            final String message = String.format("IC error: %s", ex.getMessage());
            ic.takeScreenshot();
            fail(message);
        } finally {
            ic.getDriver().close();

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
    public void checkDBData() throws Exception {
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


    @Test
    public void addReferenceTable() throws Exception {
        IC ic = new IC(props);
        try {
            ic.locateTable(IC.AllTables.VIP_БИК_СЧЁТ)
                    .addRecord()
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$БИК, "123456789")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$СЧЁТ, "12345678912345678912")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$ПРИЧИНА_ЗАНЕСЕНИЯ, "Автоматическая обработка")
                    .save();
        } finally {
            ic.close();
        }
    }

    @Test
    public void editReferenceTable() throws Exception {
        IC ic = new IC(props);
        try {
            ic.locateTable(IC.AllTables.VIP_БИК_СЧЁТ)
                    .selectRecord("123456789", "123456789123")
                    .edit()
                    //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
                    .sleep(0.5)
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$БИК, "987654321")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$СЧЁТ, "98765432198765432198")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$ПРИЧИНА_ЗАНЕСЕНИЯ, "Автоматическая обработка")
                    .save();
        } finally {
            ic.close();
        }
    }

    @Test
    public void testSelectRowByJava() throws Exception {
        try (IC ic = new IC(props)) {
            ic.locateTable(IC.AllTables.VIP_БИК_СЧЁТ);
            final RemoteWebDriver d = ic.getDriver();

            /*
            для элементов заголовка - перебирать tr[1] через th[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[4]//span

            для элементов таблицы - перебирать tr[2-*] через td[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[2]/td[4]//span
             */


            final String ROW = "%row%";
            final String COL = "%col%";
            final String thXpath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[*]//span";
            final String tdXpath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[%col%]//span";


            final String[] heads = d.findElementsByXPath(thXpath.replaceAll(ROW, "1").replaceAll(COL, "*"))
                    .stream()
                    .map(WebElement::getText)
                    .toArray(String[]::new);
            final int rowCount = d.findElementsByXPath("//div[@class='panelTable af_table']//table[2]/tbody/tr[*]")
                    .size() - 1; //первая строка - заголовок


            final String[][] data = new String[rowCount][heads.length];
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < heads.length; j++) {
                    final String xpath = tdXpath
                            .replaceAll(ROW, String.valueOf(i + 2))  //начина со второй (по меркам XPath) строки
                            .replaceAll(COL, String.valueOf(j + 4));
                    data[i][j] = d.findElementByXPath(xpath  //данные начинаются с 4 столбца
                    ).getText();
                }
            }

            System.out.println(String.join("\t",heads));
            for (String[] row : data) {
                System.out.println(String.join("\t", row));
            }
        }

    }

}
