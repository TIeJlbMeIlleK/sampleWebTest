package ru.iitgroup.tests.webdriver;

public enum AllFields {

    VIP_БИК_СЧЁТ$БИК("Бик банка VIP:"),
    VIP_БИК_СЧЁТ$СЧЁТ("Счет получатель VIP:")
    ;

    public final String heading;

    AllFields(String heading) {

        this.heading = heading;
    }
}
