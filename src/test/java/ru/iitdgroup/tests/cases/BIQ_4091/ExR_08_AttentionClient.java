package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
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

public class ExR_08_AttentionClient extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_08_AttentionClient";
    private static final String  TABLE = "(Rule_tables) Список клиентов с пометкой особое внимание";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);





    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Генерация клиентов"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
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
            description = "Настройка и включение правила",
            dependsOnMethods = "client"
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

        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
    }

    @Test(
            description = "Добавить клиента № 1 в справочник \"Список клиентов с пометкой особое внимание\" и установить флаг \"Признак \"Особое внимание\".",
            dependsOnMethods = "enableRules"
    )

    public void step1() {
        getIC().locateTable(TABLE)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(0))
                .fillCheckBox("Признак «Особое внимание»:",true)
                .save();
    }
    @Test(
            description = "Добавить клиента № 2 в справочник \"Список клиентов с пометкой особое внимание\", не устанавливать флаг \"Признак \"Особое внимание\".",
            dependsOnMethods = "step1"
    )
    public void step2() {
        getIC().locateTable(TABLE)
                .addRecord()
                .fillUser("Клиент:", clientIds.get(1))
                .fillCheckBox("Признак «Особое внимание»:",false)
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию от имени клиента № 1, 2.",
            dependsOnMethods = "step2"
    )
    public void step3() {
        commandServiceMock.run();
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ATTENTION_CLIENT);
    }

    @Test(
            description = "Провести транзакцию от имени клиента № 1, 2.",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
