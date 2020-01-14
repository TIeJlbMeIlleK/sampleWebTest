package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
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

public class GR_18_GrayBIK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_18_GrayBank";
    private String GRAY_BIK = "755225951";
    private String WHITE_BIK = "755225955";

    private static final String  TABLE = "(Rule_tables) Подозрительный банк";
    private static final String  TABLE_2 = "(Rule_tables) Подозрительные системы денежных переводов";




    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.JANUARY, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Генерация клиентов"
    )
    public void client() {
        System.out.println("Правило GR_18 срабатывает при наличии в справочнике \"Подозрительный банк\" БИК банка ТК№ 31 BIQ2296");
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
                .save()
                .sleep(15);
    }

    @Test(
            description = "Настройка и включение правила",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("БИК:",GRAY_BIK)
                .save();

        Table.Formula rows1 = getIC().locateTable(TABLE_2).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(TABLE_2)
                .addRecord()
                .fillInputText("Наименование системы:","QIWI")
                .save();
        getIC().close();

    }

    @Test(
            description = "Провести транзакцию № 1 \"Перевод на счет\" на подозрительный БИК",
            dependsOnMethods = "editReferenceData"
    )
    public void step1() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(GRAY_BIK);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAYBIK_BANK);
    }
    @Test(
            description = "Провести транзакцию № 2 \"Перевод на счет\" не на подозрительный БИК",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(WHITE_BIK);

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод через систему денежных переводов\" от подозрительной системы денежных переводов",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionSPD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getMTSystemTransfer()
                .withMtSystem("QIWI");

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAY_SYSTEM);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод через систему денежных переводов\" не от подозрительной системы денежных переводов",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionSPD();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getMTSystemTransfer()
                .withMtSystem("PAYtoPAY");

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionSPD() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }


}
