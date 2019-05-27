package ru.iitgroup.tests.webdriver;

@SuppressWarnings("NonAsciiCharacters")
public enum AllFields {

    VIP_БИК_СЧЁТ$БИК("Бик банка VIP:"),
    VIP_БИК_СЧЁТ$СЧЁТ("Счет получатель VIP:"),
    VIP_БИК_СЧЁТ$ПРИЧИНА_ЗАНЕСЕНИЯ("Причина занесения:");

    public final String heading;

    AllFields(String heading) {
        this.heading = heading;
    }
}
