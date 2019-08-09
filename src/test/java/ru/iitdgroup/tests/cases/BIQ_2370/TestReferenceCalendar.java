package ru.iitdgroup.tests.cases.BIQ_2370;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

public class TestReferenceCalendar extends RSHBCaseTest {

    private static final String CALENDAR = "(Rule_tables) Производственный календарь";
    private static final String RULE_NAME = "R01_GR_25_SeriesTransfersAndPayments";


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
//        TODO требуется реализовать импорт файлов из файловой системы

        System.out.println("\"Импорт данных из CSV-файла по шаблону: weekendDay и редактирование записей\" -- BIQ2370" + " ТК№9");
    }



    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

}
