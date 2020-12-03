package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.PCDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_15_NonTypicalGeoPosition extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String Table_Flags = "(Policy_parameters) Параметры обработки справочников и флагов";


    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);

    public static void main(String[] args) {
        CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
        commandServiceMock.run();
    }
    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(2);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(30);
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
        Table.Formula rows = getIC().locateTable("(Rule_tables) Карантин месторасположения").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceTable"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "Провести транзакции № 1 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        commandServiceMock.run();
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getPC().setUserAgent("555");
        transactionData.getClientDevice().getPC().setIpAddress("178.219.186.12");
        transactionData.getClientDevice().getPC().setBrowserData("Browser");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }


    @Test(
            description = "Провести транзакции № 2 с IP-адреса № 1 для Клиента № 1",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getPC().setUserAgent("555");
        transactionData.getClientDevice().getPC().setIpAddress("178.219.186.12");
        transactionData.getClientDevice().getPC().setBrowserData("Browser");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Провести транзакции № 3 с IP-адреса № 1 для Клиента № 1 через 2 часа",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.HOUR,49);
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getPC().setUserAgent("555");
        transactionData.getClientDevice().getPC().setIpAddress("178.219.186.12");
        transactionData.getClientDevice().getPC().setBrowserData("Browser");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION_FOR_IP);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
