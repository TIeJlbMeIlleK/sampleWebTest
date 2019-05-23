package ru.iitgroup.tests.webdriver;

import org.testng.annotations.Test;
import static  org.testng.Assert.*;
import static ru.iitgroup.tests.webdriver.ReferenceTable.FIRST_ROW;

import java.util.List;

public class ReferenceTableTest {

    @Test
    public void testMatchedRows() {

        ReferenceTable t = new ReferenceTable(null);


        final String[] heads = {"ID", "Comment", "Бик банка VIP", "Причина занесения", "Счет получатель VIP", "Дата занесения записи"};
        t.setHeads(heads);

        final String data[][] = {
                {"00001", "kjh", "44525593", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00002", "kjh", "44525594", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00003", "kjh", "44525595", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00004", "kjh", "44525596", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00005", "kjh", "44525597", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00006", "kjh", "44525598", "4,08E+19", "11.02.2019", "16:32:53"},
                {"00007", "123", "", "", "14.05.2019", "20:26:46"},
                {"00038", "", "987654321", "98765432198765432198", "14.05.2019", "21:45:33"},
                {"00039", "", "987654321", "98765432198765432198", "14.05.2019", "22:05:40"},
                {"00040", "", "123456789", "12345678912345678912", "30.04.2019", "18:15:12"},
                {"00041", "", "123456789", "12345678912345678912", "30.04.2019", "18:34:20"}
        };

        t.setData(data);

        final List<Integer> rowNums = t.findRowsBy()
                .match(heads[2], data[7][2])
                .match(heads[3], data[7][3])
                .getMatchedRows()
                .get();

        assertEquals(rowNums.size(),2);
        assertEquals(rowNums.get(0).intValue(),7+FIRST_ROW);
        assertEquals(rowNums.get(1).intValue(),8+FIRST_ROW);
    }
}