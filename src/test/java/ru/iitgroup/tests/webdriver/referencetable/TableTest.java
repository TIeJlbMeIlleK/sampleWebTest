package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.iitgroup.tests.properties.TestProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static ru.iitgroup.tests.webdriver.referencetable.Table.FIRST_ROW;

public class TableTest {
    private ChromeDriver driver;


    @BeforeClass
    public void setUp() throws IOException {
        TestProperties props = new TestProperties();
        props.load(new FileInputStream("resources/test.properties"));
        System.setProperty("webdriver.chrome.driver", props.getChromeDriverPath());
        driver = new ChromeDriver();
    }

    @AfterClass
    public void tearDown() {
        driver.close();
        driver = null;
    }

    @Test
    public void testMatchedRows() {

        Table t = new Table(driver);

        final String[] heads = {"ID", "Comment", "Бик банка VIP", "Причина занесения", "Счет получатель VIP", "Дата занесения записи"};
        t.setHeads(heads);

        final String[][] data = {
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
                .calcMatchedRows()
                .getTableRowNums();

        assertEquals(rowNums.size(), 2);
        assertEquals(rowNums.get(0).intValue(), 7 + FIRST_ROW);
        assertEquals(rowNums.get(1).intValue(), 8 + FIRST_ROW);
    }
}