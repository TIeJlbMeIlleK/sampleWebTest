package ru.iitgroup.rshbtest;


import org.testng.annotations.Test;
import ru.iitgroup.tests.webdriver.referencetable.Table;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class TableTests extends RSHBTests {

    private static final String BIC = "991234567";
    private final String NAME = "(Rule_tables) VIP клиенты БИКСЧЕТ";
    private String ACCOUNT = "99123456789123456789";
    private String NEWACCOUNT = "99123456789123456789";
    private final String CAUSE = "Автоматическая обработка";

    @Test( description = "Пример добавления записи")
    public void addRecord() throws Exception {

        ic.locateTable(NAME)
                .addRecord()
                .fillMasked("Бик банка VIP:", BIC)
                .fillMasked("Счет получатель VIP:", ACCOUNT)
                .fillMasked("Причина занесения:", CAUSE)
                .save();
    }

    @Test( description = "Пример редактирования записи", dependsOnMethods = {"addRecord"})
    public void editRecord() throws Exception {

        ic.locateTable(NAME)
                //.selectRecord("123456789", "123456789123")
                .findRowsBy()
                .match("Бик банка VIP", BIC)
                .match("Счет получатель VIP", ACCOUNT)
                .edit()
                .fillMasked("Счет получатель VIP:", NEWACCOUNT)
                .fillMasked("Причина занесения:", CAUSE)
                //.sleep(2)
                .save();
    }

    @Test( description = "Пример удаления записи", dependsOnMethods = {"editRecord"})
    public void testDeleteRecord() throws Exception {

        ic.locateTable(NAME)
                .findRowsBy()
                .match("Бик банка VIP", BIC)
                .match("Счет получатель VIP", NEWACCOUNT)
                .delete();
    }

    @Test( description = "Частный тест выбора строчек в таблице")//, enabled = false)
    public void testSelectRowByJava() throws Exception {

        final Table table =
                ic.locateTable(NAME);

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
}
