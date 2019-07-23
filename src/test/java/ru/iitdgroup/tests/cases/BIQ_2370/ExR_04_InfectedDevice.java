package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.ves.mock.VesMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_04_InfectedDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_04_InfectedDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String TABLE_2 = "(Policy_parameters) Параметры обработки событий";




    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 8, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private VesMock vesMock = getVesMock();

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
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detach("Коды ответов ВЭС")
                .attach("Коды ответов ВЭС","Идентификатор кода","Equals","46");
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceTable() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Время ожидания между получением Аутентификации клиента и вызовом ВЭС, секунд")
                .click()
                .edit()
                .fillInputText("Значение:", "5")
                .save();

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Период за который наполняется кэш для данных от ВЭС")
                .click()
                .edit()
                .fillInputText("Значение:", "1440")
                .save();

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Время ожидания актуальных данных от ВЭС")
                .click()
                .edit()
                .fillInputText("Значение:", "300")
                .save();
    }

    @Test(
            description = " В Параметрах обработки событий добавлена новая запись Покупка страховки держателей карт",
            dependsOnMethods = "editReferenceTable"
    )
    public void addNewTransactionType() {

        getIC().locateTable(TABLE_2)
                .addRecord()
                .select("Тип транзакции:","Покупка страховки держателей карт")
                .select("Наименование канала ДБО:","Мобильный банк")
        .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "addNewTransactionType"
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
            description = "Провести транзакцию Покупка страховки держателей карт № 1 в интернет-банке",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        vesMock = getVesMock();
        vesMock.setVesExtendResponse(vesMock
                .getVesExtendResponse()
                .replaceAll("\"type_id\": \"7\"","\"type_id\": \"46\""));
        vesMock.run();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ALERT_FROM_VES);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/BUYING_INSURANCE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private static VesMock getVesMock() {
        return VesMock.create().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");
    }
}
