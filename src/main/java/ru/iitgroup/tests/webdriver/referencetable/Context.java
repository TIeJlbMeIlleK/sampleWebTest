package ru.iitgroup.tests.webdriver.referencetable;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitgroup.tests.webdriver.ic.AbstractViewContext;
import ru.iitgroup.tests.webdriver.ic.ICXPath;

import java.util.ArrayList;
import java.util.List;

public class Context extends AbstractViewContext<Context> {

    public static final int FIRST_ROW = 2; //данные в IC начинаются со 2-ой строчки
    public static final int FIRST_COL = 4; //данные в IC начинаются с 4-ой колонки
    private final String ROW = "%row%";
    private final String COL = "%col%";
    private final String thXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[1]/th[*]//span";
    private final String tdXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[%col%]//span";
    private final String allRowsXPath = "//div[@class='panelTable af_table']//table[2]/tbody/tr[*]";
    private final String firstColXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[4]//span";
    private final String checkBoxXPath = "//div[@class='panelTable af_table']/table[2]/tbody/tr[%row%]/td[1]/input";

    private String[] heads;
    private String[][] data;

    public Context(RemoteWebDriver driver) {
        super(driver);
    }

    @Override
    protected Context getSelf() {
        return this;
    }

    public Context readData() {
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

        final int rowCount = driver.findElementsByXPath(allRowsXPath)
                .size() - 1; //первая строка - заголовок

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
                data[i][j] = driver.findElementByXPath(xpath
                ).getText().trim();
            }
        }
        return this;
    }

    /**
     * Добавить запись
     *
     * @return
     */
    public EditContext addRecord() {
        icxpath()
                .element("Actions")
                .preceding(ICXPath.WebElements.IMG)
                .click();
        return new EditContext(driver);
    }

    /**
     * Щёлкнуть на стрке в таблице
     *
     * @param technicalRow точный (в терминах XPath) номер строки, на который кликать.
     *                     В IC - начиная со второй
     * @return
     */
    public RecordContext click(int technicalRow) {
        final String xpath = firstColXPath.replaceAll(ROW, String.valueOf(technicalRow));
        driver.findElementByXPath(xpath).click();
        return new RecordContext(driver);
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
    public Context select(int technicalRow) {
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
    public Context delete(int technicalRow) {
        if (data == null) readData();
        select(technicalRow);
        return delete();
    }

    public Context delete() {
        driver.findElementByXPath("//span[text()='Actions']").click();
        driver.findElementByXPath("//div[contains(@class,'qtip') and contains(@aria-hidden, 'false')]//div[@class='qtip-content']/a[text()='Delete']").click();
        driver.findElementsByXPath("//button[2]/span[text()='Yes']")
                .forEach(e -> System.out.println(String.format("Displayed: %b, Enabled: %b, Text: %s", e.isDisplayed(), e.isEnabled(), e.getText())));
        sleep(5);

        return this;
    }

    public class Formula {
        private final List<Expression> matches = new ArrayList<>();
        private MatchedRows matchedRows;

        public Formula match(String colHeading, String rowText) {
            matches.add(new Expression(colHeading, rowText));
            return this;
        }

        public MatchedRows getMatchedRows() {
            return matchedRows == null ? new MatchedRows(matches) : matchedRows;
        }

        public RecordContext click() {
            return Context.this.click(getMatchedRows().get(1));
        }

        public Context select() {
            for (Integer rowNum : getMatchedRows().get()) {
                Context.this.select(rowNum);
            }
            return Context.this;
        }

        public EditContext edit() {
            return click().edit();
        }

        public Context delete() {
            return select().delete();
        }

    }

    public class MatchedRows {
        final List<Integer> matchedRows;

        public MatchedRows(List<Expression> expressions) {
            matchedRows = new ArrayList<>();
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
                    matchedRows.add(row + FIRST_ROW); //в виде, пригодном для XPath
                }
            }
        }

        public Integer get(int index) {
            return matchedRows.get(index);
        }

        public List<Integer> get() {
            return matchedRows;
        }
    }

    public class Expression {
        private final String colHeading;
        private final String rowText;

        Expression(String colHeading, String rowText) {
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
