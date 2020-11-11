package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
//TODO требуется реализовать отправку сообщения через новый ВЭС

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class R01_W6_WhiteRule_VES_TRIGGERED extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_06_VES";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
//TODO требуется реализовать отправку сообщения через новый ВЭС

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {

        System.out.println("R01_W6_Whiterule_VES. Проверка на не соответствие значениям ответа VES со справочником \"Коды ответов ВЭС\" -- BIQ2370 "+"ТК №36");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:","2000")
                .save()
                .attach("Коды ответов ВЭС","Идентификатор кода","Equals","15")
                .sleep(10);
    }@Test(
            description = "Включить интеграцию с VES",
            dependsOnMethods = "enableRules"
    )

    public void enableVES(){
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по суждения . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Время ожидания актуальных данных от ВЭС")
                .click()
                .edit()
                .fillInputText("Значение:", "300").save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableVES"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакцию №1, любую по Клиенту №1 КОд ответа от VES = значению из справочника и значению указанному в Правиле",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        System.out.println("\"R01_W6_Whiterule_VES.\n" +
                        "Проверка на соответствие значениям ответа VES со справочником \"Коды ответов ВЭС\" -- BIQ2370 " + "ТК №21");

//TODO требуется реализовать отправку сообщения через новый ВЭС

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, GOOD_VES_CODE);

        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detach("Коды ответов ВЭС");
        getIC().close();

    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
