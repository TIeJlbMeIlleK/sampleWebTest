package ru.iitdgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractView;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.fail;

public class Table extends AbstractView<Table> {

    public static final int FIRST_ROW = 2; //данные в IC начинаются со 2-ой строчки
    public static final int FIRST_COL = 4; //данные в IC начинаются с 4-ой колонки
    private final String ROW = "%row%";
    private final String COL = "%col%";
    private final String thXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[*]//span";
    private final String tdXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[%col%]";
    private final String allRowsXPath = "//div[@class='panelTable af_table']//table[2]/tbody/tr[*]";
    private final String firstColXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[4]//span";
    private final String checkBoxXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[1]/input";

    private String[] heads;
    private String[][] data;

    public Table(RemoteWebDriver driver) {
        super(driver);
    }

    public Table readData() {
        /*
            для элементов заголовка - перебирать tr[1] через th[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[4]//span

            для элементов таблицы - перебирать tr[2-*] через td[*]
            //div[@class='panelTable af_table']/table[2]/tbody/tr[2]/td[4]//span
            */

        if (data != null) return this;

        heads = driver.findElementsByXPath(thXPath)
                .stream()
                .map(WebElement::getText)
                .toArray(String[]::new);

        final int rowCount = driver.findElementsByXPath(allRowsXPath).size() - 1; //первая строка - заголовок

        if (rowCount == -1) {
            data = new String[0][heads.length];
            return this;
        }

        data = new String[rowCount][heads.length];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < heads.length; j++) {
                //FIXME: ускорить загрузку данных - загружать по колонкам, через XPath
                /*
                div[@class='panelTable af_table']/table[2]/tbody/tr[*]/td[%col%]//span
                 */
                final String xpath = tdXPath
                        .replaceAll(ROW, String.valueOf(i + FIRST_ROW))  //начина со второй  строки
                        .replaceAll(COL, String.valueOf(j + FIRST_COL)); //данные начинаются с четвёртого столбца
                data[i][j] = driver.findElementByXPath(xpath).getText().trim();
            }
        }
        return this;
    }

    /**
     * Добавить запись
     *
     * @return
     */
    public TableEdit addRecord() {
        icxpath()
                .element("Actions")
                .preceding(ICXPath.WebElements.IMG)
                .click();
        waitUntil("//a[@id='btnSave']");
        return new TableEdit(driver);
    }

    /**
     * Щёлкнуть на строке в таблице
     *
     * @param technicalRow точный (в терминах XPath) номер строки, на который кликать.
     *                     В IC - начиная со второй
     * @return
     */
    public Record click(int technicalRow) {
        final String xpath = firstColXPath.replaceAll(ROW, String.valueOf(technicalRow));
        driver.findElementByXPath(xpath).click();
        return new Record(driver);
    }

    public Record click(String itemName) {
        driver.findElementByXPath(String.format("//div[@title='%s'", itemName)).click();
        waitUntil("//img[@class='ToolbarButton editRuleMain']");
        return new Record(driver);
    }

    /**
     * Войти в контекст задания условий отбора строчек
     *
     * @return
     */
    public Formula findRowsBy() {
        readData();
        return new Formula();
    }

    /**
     * Выбрать строчку в таблице (установить checkbox)
     *
     * @param technicalRow точный (в терминах XPath) номер строки, на который кликать.
     *                     В IC - начиная со второй
     * @return
     */
    public Table select(int technicalRow) {
        final String xpath = checkBoxXPath.replaceAll(ROW, String.valueOf(technicalRow));
        WebElement cbx = driver.findElementByXPath(xpath);
        cbx.click();
        return this;
    }

    /**
     * Удалить запись из таблицы
     *
     * @param technicalRow точный (в терминах XPath) номер строки, на который кликать.
     *                     В IC - начиная со второй
     * @return
     */
    public Table delete(int technicalRow) {
        if (data == null) readData();
        select(technicalRow);
        return delete();
    }

    //FIXME: тут вообще какой-то мусор
    public Table delete() {
        driver.findElementByXPath("//span[text()='Actions']").click();
        driver.findElementByXPath("//div[contains(@class,'qtip') and contains(@aria-hidden, 'false')]//div[@class='qtip-content']/a[text()='Delete']").click();
        driver.findElementByXPath("//button[2]/span[text()='Yes']").click();
        waitUntil("//*[contains(text(),'Operation succeeded') and @class='globalMessagesInfo']");

        return this;
    }

    /**
     * Класс для формулы - набора выражений, т.е. фильтра, накладывемого на таблицу для отбора строк
     * (желательно - только одной, потому что клик будет сделан на первой подходящей
     */
    public class Formula {
        private final List<Expression> expressions = new ArrayList<>();
        public MatchedRows matchedRows;

        public Formula match(String colHeading, String rowText) {
            expressions.add(new Expression(colHeading, rowText));
            return this;
        }

        /**
         * Выполняет щелчок на первой из строк, удовлетворяющих условию
         * @return
         */
        public Record click() {
            calcMatchedRows();
            failIfNoRows();
            return Table.this.click(matchedRows.get(1));
        }

        /**
         * Вызывает Assert#fail (чтобы тест упал) для случая, если у таблицы удовлетворяющих формуле строк
         */
        private void failIfNoRows() {
            if ( matchedRows.rows.size() == 0) {
                final String formula = expressions.stream()
                        .map(exp -> String.format("%s = %s", exp.colHeading, exp.rowText))
                        .collect(Collectors.joining(", "));

                fail(String.format("По формуле %s не удалось выбрать ни одной строки из таблицы",
                        formula
                ));
            }
        }

        public Table select() {
            calcMatchedRows();
            failIfNoRows();
            for (Integer rowNum : matchedRows.get()) {
                Table.this.select(rowNum);
            }
            return Table.this;
        }

        public TableEdit edit() {
            return click().edit();
        }

        public Table delete() {
            return select().delete();
        }

        /**
         * Обновить внутренний объект, который содержит все совпадающие строки
         */
        public Formula calcMatchedRows() {
            if (matchedRows == null) {
                matchedRows = new MatchedRows(expressions);
            }
            return this;
        }

        public List<Integer> getTableRowNums() {
            return matchedRows.rows;
        }
    }

    public class MatchedRows {
        final List<Integer> rows;

        public MatchedRows(List<Expression> expressions) {
            rows = new ArrayList<>();
            if (data == null) readData();
            for (int row = 0; row < data.length; row++) {
                boolean found = true;
                for (Expression expression : expressions) {
                /*
                Поиск номера колонки по тексту
                 */
                    Integer foundCol = null;
                    for (int col = 0; col < heads.length; col++) {
                        if (heads[col].equals(expression.colHeading)) {
                            foundCol = col;
                            break;
                        }
                    }
                    if (foundCol == null) {
                        throw new IllegalStateException(String.format("Не найдена колонка с названием [%s]", expression.colHeading));
                    }
                    if (!data[row][foundCol].equals(expression.rowText)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    rows.add(row + FIRST_ROW); //в виде, пригодном для XPath
                }
            }
        }

        public Integer get(int index) {
            return rows.get(index - 1);
        }

        public List<Integer> get() {
            return rows;
        }
    }

    /**
     * Класс для выражения - одной строки фильтра для выбора строчек
     * <p/>Выражение ::= < Заголовок столбца >, < Текст в столбце >
     */
    public class Expression {
        private final String colHeading;
        private final String rowText;

        Expression(String colHeading, String rowText) {
            this.colHeading = colHeading;
            this.rowText = rowText;
        }
    }

    @Override
    protected Table getSelf() {
        return this;
    }

    /**
     * Утилитарный метод для ручного заполнения заголовков таблицы.
     * Только для тестов
     * @param data квадратный массив данных (столбцы * строчки)
     */

    @Deprecated
    public void setData(String[][] data) {
        this.data = data;
    }

    /**
     * Утилитарный метод для ручного заполнения заголовков таблицы.
     * Только для тестов
     * @param heads список заголовков
     */
    @Deprecated
    public void setHeads(String[] heads) {
        this.heads = heads;
    }
}
