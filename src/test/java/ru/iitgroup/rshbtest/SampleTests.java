package ru.iitgroup.rshbtest;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;
import ru.iitgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitgroup.tests.apidriver.ICMalfunctionError;
import ru.iitgroup.tests.apidriver.Transaction;
import ru.iitgroup.tests.dbdriver.Database;
import ru.iitgroup.tests.webdriver.RuleTemplate;
import ru.iitgroup.tests.webdriver.Table;
import ru.iitgroup.tests.webdriver.ic.IC;
import ru.iitgroup.tests.webdriver.referencetable.AllFields;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class SampleTests extends RSHBTests {

    @Test
    public void enableRule() {
        IC ic = new IC(props);
        try {
            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    //FIXME: что-то в IC не успевает отрабатывать, и надо бы ловить это не задержкой по времени, а появлением соответствующего элемента на странице
                    .selectRule("R01_ExR_04_InfectedDevice")
                    .sleep(1)
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
    public void addRecord() throws Exception {
        IC ic = new IC(props);
        try {
            ic.locateTable(Table.VIP_CLIENTS_BIC_ACCOUNT)
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
    public void editRecord() throws Exception {
        IC ic = new IC(props);
        try {
            ic.locateTable(Table.VIP_CLIENTS_BIC_ACCOUNT)
                    //.selectRecord("123456789", "123456789123")
                    .findRowsBy()
                    .match("Бик банка VIP", "987654321")
                    .match("Счет получатель VIP", "98765432198765432198")
                    .edit()
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$БИК, "987654322")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$СЧЁТ, "98765432198765432198")
                    .fillMasked(AllFields.VIP_БИК_СЧЁТ$ПРИЧИНА_ЗАНЕСЕНИЯ, "Автоматическая обработка")
                    //.sleep(2)
                    .save();
        } catch (Exception e) {

            throw new ICMalfunctionError(e, ic.takeScreenshot());
        } finally {
            ic.close();
        }
    }


    @Test
    public void testDeleteRecord() throws Exception {
        IC ic = new IC(props);
        try {
            ic.locateTable(Table.VIP_CLIENTS_BIC_ACCOUNT)
                    .findRowsBy()
                    .match("ID", "00044")
                    .delete();
        } catch (Exception e) {
            throw new ICMalfunctionError(e, ic.takeScreenshot());
        } finally {

            ic.close();
        }
    }

    @Test
    public void testSelectRowByJava() throws Exception {
        try (IC ic = new IC(props)) {
            final ru.iitgroup.tests.webdriver.referencetable.Context referenceTable =
                    ic.locateTable(Table.VIP_CLIENTS_BIC_ACCOUNT);

            referenceTable.readData();
//            System.out.println(String.join("\t",referenceTable.heads));
//            for (String[] row : referenceTable.data) {
//                System.out.println(String.join("\t", row));
//            }

            ru.iitgroup.tests.webdriver.referencetable.Context.Formula rm =
                    referenceTable.findRowsBy()
                            .match("Бик банка VIP", "987654321")
                            .match("Счет получатель VIP", "98765432198765432198");
//                    .match("Comment","123");

            rm.select()
                    .sleep(2);
            //TODO: дописать тестовый код, позволяющий убедиться в том, что строки действительно выбрались, а пока - смотреть глазами

            final List<Integer> foundRows = rm.getMatchedRows().get();
            assertEquals(foundRows.size(), 2);

            referenceTable.click(foundRows.get(0));

            referenceTable.sleep(2);
        }
    }

    @Test(description = "Пример теста загрузки правил на экранной форме импорта правил")
    public void importRulesThroughImportRuleTablesForm() {
        IC ic = new IC(props);

        ic.locateImportRuleTable()
                .chooseTable(Table.VIP_CLIENTS_BIC_ACCOUNT)
                .chooseFile("VIP клиенты БИКСЧЕТ.csv")
                .load()
                .rollback();
    }

    @Test(description = "Пример теста создания правила на экранной форме правил")
    public void createRuleThroughRulesForm() {
        IC ic = new IC(props);
        ic.locateRules()
                .createRule(RuleTemplate.BR_01_PayeeInBlackList)
                .fillInputText("Name:", "name")
                .fillCheckBox("Active:", true)
                .save();
    }
}
