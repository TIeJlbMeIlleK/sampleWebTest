package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.PCDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_15_NonTypicalGeoPosition_NewTransaction extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String Table_Flags = "(Policy_parameters) Параметры обработки справочников и флагов";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_2 = "(Policy_parameters) Параметры обработки событий";

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("\"Правило GR_15_NonTypicalGeoPosition работает с новым типом транзакций \"Покупка страховки держателей карт \"\" -- BIQ2370" + " ТК№4");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Настроить Время после добавления в карантин",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceTable() {
        getIC().locateTable(Table_Flags)
                .findRowsBy()
                .match("Описание", "Время после добавления в карантин")
                .click()
                .edit()
                .fillInputText("Значение:", "2")
                .save();
    }

    @Test(
            description = " В Параметрах обработки событий добавлена новая запись Покупка страховки держателей карт",
            dependsOnMethods = "editReferenceTable"
    )
    public void addNewTransactionType() {
        Table.Formula rows = getIC().locateTable(TABLE_2).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
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
            description = "Провести транзакцию Покупка страховки держателей карт № 1 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.PC);
        transactionData.getClientDevice().setPC(new PCDevice());
        transactionData.getClientDevice().getPC().setUserAgent("555");
        transactionData.getClientDevice().getPC().setIpAddress("178.219.186.12");
        transactionData.getClientDevice().getPC().setBrowserData("Browser");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }


    @Test(
            description = "Провести транзакцию Покупка страховки держателей карт № 2 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.PC);
        transactionData.getClientDevice().setPC(new PCDevice());
        transactionData.getClientDevice().getPC().setUserAgent("555");
        transactionData.getClientDevice().getPC().setIpAddress("178.219.186.12");
        transactionData.getClientDevice().getPC().setBrowserData("Browser");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
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
}
