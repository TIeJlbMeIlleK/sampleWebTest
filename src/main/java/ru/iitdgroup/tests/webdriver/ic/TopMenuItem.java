package ru.iitdgroup.tests.webdriver.ic;

/**
 * Список элементов меню Investigation center.
 */
public enum TopMenuItem {

    ALERTS("Alerts"),
    REPORTS("Reports"),
    REPORT_MANAGEMENT("Report Management"),
    REFERENCE_DATA("Reference Data"),
    ANALYTICS("Analytics"),
    SCORING_MODELS("Scoring Models"),
    RULES("Rules"),
    IMPORT_RULE_TABLES("Import Rule Tables");

    private final String heading;

    TopMenuItem(String heading) {
        this.heading = heading;
    }

    public String getHeading() {
        return this.heading;
    }
}
