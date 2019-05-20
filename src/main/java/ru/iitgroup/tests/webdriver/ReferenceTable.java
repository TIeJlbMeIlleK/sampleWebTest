package ru.iitgroup.tests.webdriver;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.ArrayList;
import java.util.List;

public class ReferenceTable extends ICView {

    private final String ROW = "%row%";
    private final String COL = "%col%";
    private final String thXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[*]//span";
    private final String tdXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[%col%]//span";
    private final String allRowsXPath = "//div[@class='panelTable af_table']//table[2]/tbody/tr[*]";
    private final String firstColXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[4]//span";
    private final String checkBoxXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[1]/input";

    public String[] heads;
    public String[][] data;

    public ReferenceTable(RemoteWebDriver driver) {
        super(driver);

    }

    public ReferenceTableEdit addRecord() {
        icxpath()
                .element("Actions")
                .preceding(ICXPath.WebElements.IMG)
                .click();
        return new ReferenceTableEdit(driver);
    }

    public ReferenceTable readData() {
        /*
            для элементов заголовка - перебирать tr[1] через th[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[4]//span

            для элементов таблицы - перебирать tr[2-*] через td[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[2]/td[4]//span
            */

        heads = driver.findElementsByXPath(thXPath)
                .stream()
                .map(WebElement::getText)
                .toArray(String[]::new);

        final int rowCount = driver.findElementsByXPath(allRowsXPath)
                .size() - 1; //первая строка - заголовок

        data = new String[rowCount][heads.length];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < heads.length; j++) {
                final String xpath = tdXPath
                        .replaceAll(ROW, String.valueOf(i + 2))  //начина со второй  строки
                        .replaceAll(COL, String.valueOf(j + 4)); //данные начинаются с четвёртого столбца
                data[i][j] = driver.findElementByXPath(xpath
                ).getText().trim();
            }
        }
        return this;
    }

    List<Integer> matchedRows(List<RowMatch> rowMatches) {
        List<Integer> matchedRows = new ArrayList<>();
        for (int row = 0; row < data.length; row++) {
            boolean found = true;
            for (RowMatch rowMatch : rowMatches) {
                /*
                Поиск номера колонки по тексту
                 */
                Integer foundCol = null;
                for (int col = 0; col < heads.length; col++) {
                    if (heads[col].equals(rowMatch.colHeading)) {
                        foundCol = col;
                        break;
                    }
                }
                if (foundCol == null) {
                    throw new IllegalStateException(String.format("Не найдена колонка с названием [%s]", rowMatch.colHeading));
                }
                if (!data[row][foundCol].equals(rowMatch.rowText)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                matchedRows.add(row);
            }
        }
        return matchedRows;
    }

    public ReferenceTable clickOn(int row) {
        final String xpath = firstColXPath.replaceAll(ROW, String.valueOf(row + 2));
        driver.findElementByXPath(xpath).click();
        return this;
    }

    public RowMatches findRowsBy() {
        return new RowMatches(this);
    }

    @Deprecated
    public ReferenceTableRecord selectRecord(String... rowValues) {
        icxpath()
                .row(rowValues)
                .click();
        return new ReferenceTableRecord(driver);

    }


    public class RowMatches {


        private final ReferenceTable parent;

        private final List<RowMatch> matches = new ArrayList<>();

        public RowMatches(ReferenceTable parent) {
            this.parent = parent;
        }

        public RowMatches match(String colHeading, String rowText) {
            matches.add(new RowMatch(colHeading, rowText));
            return this;
        }

        public ReferenceTable click() {
            return click(1);
        }

        public ReferenceTable click(int nth) {
            return clickOn(nth - 1);
        }

        public ReferenceTable select() {
            for (Integer rowNum : getAll()) {
                select(rowNum);
            }
            return parent;
        }

        public ReferenceTable select(int nth) {
            final String xpath = checkBoxXPath.replaceAll(ROW, String.valueOf(nth));
            WebElement cbx = driver.findElementByXPath(xpath);
            cbx.click();
            return parent;
        }

        List<RowMatch> get() {
            return matches;
        }

        public List<Integer> getAll() {
            return matchedRows(matches);
        }

    }

    public class RowMatch {
        private final String colHeading;
        private final String rowText;

        RowMatch(String colHeading, String rowText) {
            this.colHeading = colHeading;
            this.rowText = rowText;
        }

    }

    @Deprecated
    public void setData(String[][] data) {
        this.data = data;
    }

    @Deprecated
    public void setHeads(String[] heads) {
        this.heads = heads;
    }
}
