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


public class BR_01_PayeeInBlackList_Black_Bik extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String REFERENCE_ITEM = "(Rule_tables) Запрещенные получатели БИКСЧЕТ";

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
            description = "Занести пару БИК+СЧЕТ в справочник запрещенных",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillMasked("БИК:", "042301968")
                .fillMasked("Счет:", "50505810835620000856")
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
            description = "Провести транзакцию № 1 Перевод на счет на БИК+СЧЕТ из справочника запрещенных",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301968");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("50505810835620000856");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_BIK_IN_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 2 Перевод на счет на БИК из справочника запрещенных, а СЧЕТ - нет.",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301968");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("50505810835620000900");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 3 Перевод на счет на СЧЕТ из справочника запрещенных, а БИК - нет.",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301888");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("50505810835620000856");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
