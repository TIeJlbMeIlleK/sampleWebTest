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

public class ExR_07_IMSI_and_IMEI extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_07_Devices";
    private String IMSI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    private String IMSI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    private String IMSI_3 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_3 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    private String IMSI_4 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_4 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    private String IMSI_5 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_5 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";

    private static final String  TABLE = "(Rule_tables) Доверенные устройства для клиента";




    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.DECEMBER, 1, 0, 0, 0);
    private final GregorianCalendar time_2 = new GregorianCalendar(2019, Calendar.DECEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Генерация клиентов"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
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

        getIC().locateTable("(Policy_parameters) Параметры обработки справочников и флагов")
                .findRowsBy().match("Код значения","PERIOD_CHANGE_CUSTOMER_ATTRIBUTES").click()
                .edit()
                .fillInputText("Значение:","1")
                .save();
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
                .fillInputText("IMSI:",IMSI_1)
                .fillUser("Клиент:",clientIds.get(0))
                .fillCheckBox("Доверенный:",true)
                .fillInputText("IMEI:",IMEI_1)
                .save();
        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("IMSI:",IMSI_4)
                .fillUser("Клиент:",clientIds.get(0))
                .fillCheckBox("Доверенный:",true)
                .fillInputText("IMEI:",IMEI_4)
                .save();
        getIC().close();

    }


    @Test(
            description = "Провести транзакцию № 1 от имени клиента № 1 с устройства № 1 IMEI_1 IMSI_1",
            dependsOnMethods = "editReferenceData"
    )

    public void step1() {
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IMSI + "\n" + EXIST_TRUSTED_IMEI + "\n");
    }
    @Test(
            description = "Провести транзакцию № 2 от имени клиента № 1 с устройства № 2 IMEI_2 IMSI_1",
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
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_2);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IMSI + "\n" + NO_TRUSTED_IMEI + "\n" + NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 3 от имени клиента № 1 с устройства № 3 IMEI_1 IMSI_2",
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
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_1);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IMEI + "\n" + NO_TRUSTED_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 4 от имени клиента № 2 с устройства № 4 IMEI_5 IMSI_4",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_5);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_4);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_TRASACTION_WITH_SAME_IMSI + "\n" + NO_TRASACTION_WITH_SAME_IMEI + "\n");
    }

    @Test(
            description = "Провести транзакцию № 5 от имени клиента № 2 с устройства № 5 IMEI_4 IMSI_5",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction2();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_4);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_5);

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
