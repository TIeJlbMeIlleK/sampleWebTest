package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class BR_01_PayeeInBlackList_Black_Payment_Systems extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String REFERENCE_ITEM_BLACK_RULE_TABLE = "(Rule_tables) Запрещенные получатели систем денежных переводов";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(3);
    }

    @Test(
            description = "Занести в справочник Запрещенные получатели систем денежных переводов номер (идентификатор) получателя в системе денежных переводов",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_BLACK_RULE_TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_BLACK_RULE_TABLE)
                .addRecord()
                .fillMasked("Наименование получателя:", "Иванов Иван Иванович")
                .fillMasked("Система денежных переводов:","QIWI")
                .fillMasked("Страна получателя:","Россия")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceData"
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1 Перевод через систему денежных переводов на получателя не из справочника (при этом Система денежных переводов и страна получателя числятся в списке запрещенных)",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getMTSystemTransfer()
                .setMtSystem("QIWI");
        transactionData
                .getMTSystemTransfer()
                .setReceiverCountry("Россия");
        transactionData
                .getMTSystemTransfer()
                .setReceiverName("Ахметов Ильзат Эльдарович");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }

    @Test(
            description = "Перевод через систему денежных переводов на получателя из справочника (Система денежных переводов и страна получателя числятся в списке запрещенных)",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getMTSystemTransfer()
                .setMtSystem("QIWI");
        transactionData
                .getMTSystemTransfer()
                .setReceiverCountry("Россия");
        transactionData
                .getMTSystemTransfer()
                .setReceiverName("Иванов Иван Иванович");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BLOCK_MT_SYSTEM);
    }
    @Test(
            description = "Провести транзакцию № 1 Перевод через систему денежных переводов на получателя не из справочника (при этом Система денежных переводов и страна получателя числятся в списке запрещенных)",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionRefactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getMTSystemTransfer()
                .setMtSystem("QIWI");
        transactionData
                .getMTSystemTransfer()
                .setReceiverCountry("Россия");
        transactionData
                .getMTSystemTransfer()
                .setReceiverName("Ахметов Ильзат Эльдарович");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }

    @Test(
            description = "Перевод через систему денежных переводов на получателя из справочника (Система денежных переводов и страна получателя числятся в списке запрещенных)",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionRefactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getMTSystemTransfer()
                .setMtSystem("QIWI");
        transactionData
                .getMTSystemTransfer()
                .setReceiverCountry("Россия");
        transactionData
                .getMTSystemTransfer()
                .setReceiverName("Иванов Иван Иванович");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BLOCK_MT_SYSTEM);
    }



    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SPD.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionRefactor() {
        Transaction transaction = getTransaction("testCases/Templates/SPD_Refactor.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
