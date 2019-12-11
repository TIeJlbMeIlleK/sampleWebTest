package ru.iitdgroup.tests.cases.BIQ_4274;

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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_LastTransaction extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 1, 1, 0, 0);
    private final GregorianCalendar time1 = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_GOOD = "(Rule_tables) Доверенные получатели";
    private static final String LOCAL_TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";

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
// Чистим справочники доверенных получателей и Карантина получателей по клиентам
        Table.Formula rows = getIC().locateTable(TABLE_QUARANTINE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        Table.Formula rows1 = getIC().locateTable(TABLE_GOOD).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }

        getIC().locateTable(LOCAL_TABLE).findRowsBy().match("Код значения","TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
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
            description = "Провести транзакции № 1 Оплата услуг, сумма 10",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650551178965411");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Провести транзакции № 2 Перевод на карту, сумма 10",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.SECOND,30);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650551178965411");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Провести транзакции № 3 Перевод на счет, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Map<String, Object> values = new HashMap<>();
        values.put("TIME_STAMP", Instant.now().minus(2, ChronoUnit.DAYS).toString());

        try (Database db = getDatabase()) {
            db.updateWhere("dbo.QUARANTINE_LIST", values, "WHERE CARDNUMBER = 4650551178965411");
        } catch (Exception e) {
            e.printStackTrace();
        }

        IgniteMessaging rmtMsg = getMsg();
        rmtMsg.send("RELOAD_QUARANTINE", this.getClass().getSimpleName());
//      Для WhiteList - RELOAD_WHITE
    }

    @Test(
            description = "Провести транзакции № 4 Перевод в бюджет, сумма 10",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time1.add(Calendar.SECOND,30);
        Transaction transaction = getTransactionPhoneNumberTransfer_new();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        transactionData.getPhoneNumberTransfer()
                .setBIK(null);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(null);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4650551178965411");
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Провести транзакцию № 5 Оплата услуг, сумма 11",
            dependsOnMethods = "step4"
    )
    public void step5() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time1.getTime());

        getIC().locateTable(TABLE_QUARANTINE)
                .findRowsBy()
                .match("Номер Карты получателя","4650551178965411")
                .click();
        assertTableField("Дата последней транзакции:",s);
        getIC().close();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhoneNumberTransfer_new() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1));
        return transaction;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
