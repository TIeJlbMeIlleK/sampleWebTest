package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_Time_After_Add_To_Carantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String CARDNUMBER = "4378723743757560";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .save();

//        TODO требуется реализовать настройку блока Alert Scoring Model по правилу + Alert Scoring Model общие настройки

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.delete();}

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
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
            description = "Провести транзакцию № 0 \"Перевод по номеру телефона\" от имени клиента № 1",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber("4378723743757560");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) { rows.click();}
        assertTableField("Номер Карты получателя:","4378723743757560");
    }

    @Test(
            description = "Провести транзакцию № 1 \"Перевод по номеру телефона\" от имени клиента № 1 в пользу получателя, находящегося в карантине\n",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber("4378723743757560");

        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Изменить \"Дата занесения в карантин\" для получателя для клиента № 1 на 2 дня назад",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Map<String, Object> values = new HashMap<>();
        values.put("TIME_STAMP", Instant.now().minus(2, ChronoUnit.DAYS).toString());
        try (Database db = getDatabase()) {
            db.updateWhere("dbo.QUARANTINE_LIST", values, "WHERE CARDNUMBER = " + CARDNUMBER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод по номеру телефона\" от имени клиента № 1 в пользу получателя, находящегося в карантине",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer()
                .setDestinationCardNumber("4378723743757560");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_EXIST_QUARANTINE_LOCATION);
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
