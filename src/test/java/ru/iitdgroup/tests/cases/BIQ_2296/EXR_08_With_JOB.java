package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.ignite.IgniteMessaging;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.dbdriver.Database;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class EXR_08_With_JOB extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_08_AttentionClient";
    private static final String  TABLE = "(Rule_tables) Список клиентов с пометкой особое внимание";

    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    @Test(
            description = "Генерация клиентов"
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
            description = "Настройка и включение правила",
            dependsOnMethods = "client"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save();

        getIC().locateTable("(Policy_parameters) Параметры обработки справочников и флагов")
                .findRowsBy().match("Код значения","CLAIM_PERIOD")
                .click().edit().fillInputText("Значение:","2").save();

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

        Map<String, Object> values = new HashMap<>();
        values.put("created", Instant.now().minus(3, ChronoUnit.DAYS).toString());

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.ATTENTION_CLIENTS", values, "WHERE ATTENTION_FLAG = 1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test(
            description = "Запустить джоб UpdateAttentionClients",
            dependsOnMethods = "step1"
    )
    public void step2() {
        getIC().locateJobs()
                .selectJob("UpdateAttentionClients")
                .run();
        getIC().close();
        IgniteMessaging rmtMsg = getMsg();
        rmtMsg.send("RELOAD_ATTENTION_CLIENT", this.getClass().getSimpleName());
    }

    @Test(
            description = "Провести транзакцию от имени клиента № 1",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
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
