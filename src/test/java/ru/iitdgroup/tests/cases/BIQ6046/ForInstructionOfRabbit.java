package ru.iitdgroup.tests.cases.BIQ6046;

import org.testng.annotations.Test;

import ru.iitdgroup.tests.cases.RSHBCaseTest;


public class ForInstructionOfRabbit extends RSHBCaseTest {


    private static final String RULE_NAME ="";

    @Test(
            description = "Настройка и включение правила"
    )
    public void sendVes() {
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("ilushka305","ilushka306")
                .replaceAll("305","306")
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf","306"));
        getRabbit().sendMessage();
        getRabbit().close();
    }
    //TODO так же через класс Рэббит по аналогии с методом sendVes можно отправлять сообщения от КАФ в определенные очереди



    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}
