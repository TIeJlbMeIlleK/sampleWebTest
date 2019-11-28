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

public class ExR_07_Devices extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_07_Devices";
    private String IMSI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMSI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMSI_3 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_3 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private static final String  TABLE = "(Rule_tables) Доверенные устройства для клиента";




    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 0, 0, 0);
    private final GregorianCalendar time_2 = new GregorianCalendar(2019, Calendar.JULY, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Генерация клиентов"
    )
    public void client() {
        try {
            for (int i = 0; i < 3; i++) {
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
                .fillInputText("Период контроля смены атрибутов клиента  (пример: 72 часа):","1")
                .save()
                .sleep(5);
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
                .fillInputText("IMSI:",IMSI_3)
                .fillUser("Клиент:",clientIds.get(0))
                .fillCheckBox("Доверенный:",true)
                .fillInputText("IMEI:",IMEI_3)
                .save();
        getIC().close();

        /// Доверенное устройство для клиента 1: IMEI = 11557984121113527 IMSI = 11315784121115541
    }


    @Test(
            description = "Провести транзакцию № 1 от имени клиента № 1 с устройства № 1 IMEI_1 IMSI_1",
            dependsOnMethods = "editReferenceData"
    )

    public void step1() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_1);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }
    @Test(
            description = "Провести транзакцию № 2 от имени клиента № 1 с устройства № 2 IMEI_2 IMSI_2",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_2);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 3 от имени клиента № 1 с устройства № 1 IMEI_1 IMSI_1",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_1);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 4 от имени клиента № 2 с устройства № 2 IMEI_2 IMSI_2",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time_2.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_2);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HAS_TRANSACTIONS);
    }

    @Test(
            description = "Провести транзакцию № 5 от имени клиента № 2 с устройства № 2 IMEI_2 IMSI_2 (прошло более часа с  транзакции № 4)",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time_2.add(Calendar.MINUTE, 70);
        Transaction transaction = getTransaction2();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_2);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = " Провести транзакцию № 6 от имени клиента № 1 с устройства № 3 IMEI_3 IMSI_3",
            dependsOnMethods = "step5"
    )
    public void step6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_3);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_3);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IMSI + "\n" + EXIST_TRUSTED_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 7 от имени клиента № 2 с устройства № 3 IMEI_3 IMSI_3",
            dependsOnMethods = "step6"
    )
    public void step7() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_3);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_3);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_EXR_07);
    }

    @Test(
            description = "Провести транзакцию № 8 от имени клиента № 3 с устройства № 3 IMEI_3 IMSI_3",
            dependsOnMethods = "step7"
    )
    public void step8() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_3);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_3);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HAS_TRANSACTIONS);
    }

    @Test(
            description = "Провести транзакцию № 9 от имени клиента № 3 с устройства № 3 IMEI_3 IMSI_3 (прошло более часа с транзакции № 8)",
            dependsOnMethods = "step7"
    )
    public void step9() {
        time.add(Calendar.MINUTE, 71);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_3);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_3);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
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
    private Transaction getTransaction2() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_2));
        return transaction;
    }

}
