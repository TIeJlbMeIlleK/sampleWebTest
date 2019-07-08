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

public class ExR_06_GrayDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_06_GrayDevice";
    private static final String TABLE_IMSI = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String TABLE_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String TABLE_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";



    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 4, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

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
                .fillCheckBox("Active:", true).save().sleep(5);


    }
    @Test(
            description = "Добавить в справочник подозрительных IMSI",
            dependsOnMethods = "enableRules"
    )
    public void editTableIMSI() {
        Table.Formula rows = getIC().locateTable(TABLE_IMSI).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IMSI)
                .addRecord()
                .fillMasked("IMSI:", "250015038779300")
                .save();
    }
    @Test(
            description = "Добавить в справочник подозрительных IMEI",
            dependsOnMethods = "editTableIMSI"
    )
    public void editTableIMEI() {
        Table.Formula rows = getIC().locateTable(TABLE_IMEI).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IMEI)
                .addRecord()
                .fillMasked("IMEI:", "863313032529520")
                .save();
    }
    @Test(
            description = "Добавить в справочник подозрительных IFV",
            dependsOnMethods = "editTableIMEI"
    )
    public void editTableIFV() {
        Table.Formula rows = getIC().locateTable(TABLE_IFV).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_IFV)
                .addRecord()
                .fillMasked("Identifier for vendor:", "3213-5F97-4B54-9A98-748B1CF8AB8C")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editTableIFV"
    )
    public void step0() {
        try {
            for (int i = 0; i < 4; i++) {
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
            description = "Выполнить регулярную транзакцию № 1 с подозрительного устройства (IMEI/IMSI/IFV)",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного IMEI",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 3 с подозрительного IMSI",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("250015038779300");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с подозрительного IMEI+IMSI",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("863313032529520");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("250015038779300");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IMSI_AMD_IMEI);
    }

    @Test(
            description = "Выполнить транзакцию № 5 с подозрительного IFV",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionIOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getClientDevice()
                .getIOS()
                .setIdentifierForVendor("3213-5F97-4B54-9A98-748B1CF8AB8C");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IFV);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_ANDROID.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionIOC() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
