package ru.iitdgroup.rshbtest;


import org.testng.annotations.Test;

import static org.testng.Assert.fail;

public class RuleTests extends RSHBTests {

    @Test(description = "Пример включения одного правила")
    public void enableRule() {

        try {
            ic.locateRules()
                    .selectVisible()
                    .deactivate()
                    .selectRule("R01_GR_04_OnePayerToManyPhones")
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

    @Test(description = "Пример создания правила")
    public void createRule() {

        ic.locateRules()
                .createRule("BR_01_PayeeInBlackList")
                .fillInputText("Name:", "__test_rule__")
                .fillCheckBox("Active:", true)
                .save();
    }

    @Test(description = "Пример редактирования правила", dependsOnMethods = {"createRule"})
    public void editRule() {

        ic.locateRules()
                .editRule("__test_rule__")
                //FIXME: не успевает отрисовываться редактор правила
                .sleep(0.5)
                .fillTextArea("Description:", "Пример описания правила")
                .save();
    }

    @Test(description = "Пример удаления правила", dependsOnMethods = {"editRule"})
    public void deleteRule() {

        ic.locateRules()
                .deleteRule("__test_rule__");
    }

    @Test(description = "Пример загрузки rule_table")
    public void importRuleTable() {

        ic.locateImportRuleTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .chooseFile("VIP клиенты БИКСЧЕТ.csv")
                .load();
    }

    @Test(description = "Пример отката загрузки rule_table", dependsOnMethods = {"importRuleTable"})
    public void rollbackRuleTable() {

        ic.locateImportRuleTable("(Rule_tables) VIP клиенты БИКСЧЕТ")
                .rollback();
    }

    @Test
    public void testChangeTab() {
        ic.locateRules()
                .openRecord("R01_GR_01_AnomalTransfer")
                .tab("Alert Scoring Models");
    }

    @Test
    public void testTabSpoilerEdit() {
        ic.locateRules()
                .openRecord("R01_GR_01_AnomalTransfer")
                .tab("Alert Scoring Models")
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .editBlock(0)
                .select(1, 0, "No");
    }

    @Test
    public void testTabSpolerSave() {
        ic.locateRules()
                .openRecord("R01_GR_01_AnomalTransfer")
                .tab("Alert Scoring Models")
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .save();
    }
}
