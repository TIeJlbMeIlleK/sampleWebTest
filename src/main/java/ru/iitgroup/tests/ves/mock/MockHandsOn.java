package ru.iitgroup.tests.ves.mock;

public class MockHandsOn {

    public static void main(String[] args) {
        VesMock vesMock = VesMock.create(8010);
        vesMock
              //  .withVesResponse("/home/nkuzin/drive/q_drive/Projects/rshb-autotest/resources/ves/ves-data.json")
               // .withVesExtendResponse("/home/nkuzin/drive/q_drive/Projects/rshb-autotest/resources/ves/ves-extended.json")
                .run();
    }
}
