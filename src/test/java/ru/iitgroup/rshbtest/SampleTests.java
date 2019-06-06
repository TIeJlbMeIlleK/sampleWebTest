package ru.iitgroup.rshbtest;


import org.testng.annotations.Test;
import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.webdriver.ic.IC;
import ru.iitgroup.tests.webdriver.referencetable.Table;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class SampleTests extends RSHBTests {

    @Test
    public void enableRule() {

        try {
            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    .selectRule("R01_ExR_04_InfectedDevice")
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


    @Test
    public void addRecord() throws Exception {

        ic.locateTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .addRecord()
                .fillMasked("Бик банка VIP:", "123456789")
                .fillMasked("Счет получатель VIP:", "12345678912345678912")
                .fillMasked("Причина занесения:", "Автоматическая обработка")
                .save();
    }

    @Test
    public void editRecord() throws Exception {

        ic.locateTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                //.selectRecord("123456789", "123456789123")
                .findRowsBy()
                .match("Бик банка VIP", "987654321")
                .match("Счет получатель VIP", "98765432198765432198")
                .edit()
                .fillMasked("Бик банка VIP:", "987654322")
                .fillMasked("Счет получатель VIP:", "98765432198765432198")
                .fillMasked("Причина занесения:", "Автоматическая обработка")
                //.sleep(2)
                .save();
    }


    @Test
    public void testDeleteRecord() throws Exception {

        ic.locateTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .findRowsBy()
                .match("ID", "00044")
                .delete();
    }

    @Test
    public void testSelectRowByJava() throws Exception {

        final Table table =
                ic.locateTable("(Rule_tables) VIP клиенты БИКСЧЕТ");

        table.readData();
//            System.out.println(String.join("\t",table.heads));
//            for (String[] row : table.data) {
//                System.out.println(String.join("\t", row));
//            }

        Table.Formula formula =
                table.findRowsBy()
                        .match("Бик банка VIP", "987654321")
                        .match("Счет получатель VIP", "98765432198765432198")
//                    .match("Comment","123");
                        .calcMatchedRows();

        formula
                .select()
                .sleep(2);
        //TODO: дописать тестовый код, позволяющий убедиться в том, что строки действительно выбрались, а пока - смотреть глазами

        final List<Integer> foundRows = formula.getTableRowNums();
        assertEquals(foundRows.size(), 1);

        table.click(foundRows.get(0));

        table.sleep(2);
    }

    @Test(description = "Загрузки rule_table")
    public void importRuleTable() {

        ic.locateImportRuleTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .chooseFile("VIP клиенты БИКСЧЕТ.csv")
                .load();
    }

    @Test(description = "Пример отката загрузки")
    public void rollbackRuleTable() {

        ic.locateImportRuleTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .rollback();
    }

    @Test(description = "Пример создания правила")
    public void createRule() {

        ic.locateRules()
                .createRule("BR_01_PayeeInBlackList")
                .fillInputText("Name:", "__test_rule__")
                .fillCheckBox("Active:", true)
                .save();
    }

    @Test(description = "Пример редактирования правила")
    public void editRule() {

        ic.locateRules()
                .editRule("__test_rule__")
                //FIXME: не успевает отрисовываться редактор правила
                .sleep(0.5)
                .fillTextArea("Description:", "Пример описания правила")
                .save();
    }

    @Test(description = "Пример удаления правила")
    public void deleteRule() {

        ic.locateRules()
                .deleteRule("__test_rule__");
    }
}
