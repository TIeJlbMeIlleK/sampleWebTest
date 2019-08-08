package ru.iitdgroup.tests.webdriver.scoringmodels;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

public class ScoringModelsTest extends RSHBCaseTest {

    @Override
    protected String getRuleName() {
        return null;
    }

    @Test
    public void testOpenRecord() {
        getIC()
                .locateScoringModels()
                .openRecord("Подозрительная транзакция")
                .openRule("R01_ExR_04_InfectedDevice")
                .edit()
                .editBlock(0)
                .add();
    }
}