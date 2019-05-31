package ru.iitgroup.tests.webdriver.ic;

/**
 * Список элементов меню Investigation center.
 */
public enum TopMenuItem {

    REFERENCE_DATA("Reference Data"),
    ANALYTICS("Analytics"),
    RULES("Rules"),
    IMPORT_RULE_TABLES("Import Rules Tables");

    private final String heading;

    TopMenuItem(String heading) {
        this.heading = heading;
    }

    public String getHeading() {
        return this.heading;
    }
}
