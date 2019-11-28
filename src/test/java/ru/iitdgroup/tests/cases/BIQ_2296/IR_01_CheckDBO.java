package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_01_CheckDBO extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_01_CheckDBO";
    private static final String TABLE_IP = "(Rule_tables) Подозрительные IP адреса";



    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true).save().sleep(5);

    }
    @Test(
            description = "1 - Открытие вклада\n" +
                    "2 - Закрытие вклада\n" +
                    "3 - Открытие текущего счета (в т.ч. накопительного)\n" +
                    "4 - Закрытие текущего счета (в т.ч. накопительного)\n" +
                    "5 - Перевод между счетами\n" +
                    "6 - Перевод на счет другому лицу\n" +
                    "7 - Перевод на карту другому лицу\n" +
                    "8 - Перевод в бюджет, оплата начислений, получаемых из ГИС ГМП\n" +
                    "9 - Оплата услуг\n" +
                    "10 - Перевод через систему денежных переводов\n" +
                    "11 - Изменение перевода, отправленного через систему денежных переводов\n" +
                    "12 - Перевод со сторонней карты на карту банка",
            dependsOnMethods = "enableRules"
    )
    public void editIP() {
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Закрытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();
        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Открытие вклада")
                .save();



    }
    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editIP"
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
            description = "Выполнить регулярную транзакцию № 1с подозрительного IP-адреса",
            dependsOnMethods = "client"
    )
    public void step1() {

    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного IP-адреса",
            dependsOnMethods = "step1"
    )
    public void step2() {

    }

    @Test(
            description = "Выполнить транзакцию № 3 не с подозрительного IP-адреса",
            dependsOnMethods = "step2"
    )
    public void step3() {

    }

    @Test(
            description = "Выполнить транзакцию № 4 с IP-адреса подозрительной маски",
            dependsOnMethods = "step3"
    )
    public void step4() {

    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
