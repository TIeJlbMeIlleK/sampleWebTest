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

public class ExR_07_Devices extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_07_Devices";
    private String IMSI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMSI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private String IMEI_2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private static final String  TABLE = "(Rule_tables) Доверенные устройства для клиента";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);





    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
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
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период контроля смены атрибутов клиента  (пример: 72 часа):","1")
                .save()
                .sleep(30);
    }

    @Test(
            description = "Добавление доверенного устройства",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("IMSI:",IMSI_2)
                .fillUser("Клиент:",clientIds.get(0))
                .fillCheckBox("Доверенный:",true)
                .fillInputText("IMEI:",IMEI_2)
                .save();
        getIC().close();
    }


    @Test(
            description = "Провести транзакцию № 1 от имени клиента № 1 с устройства № 1 IMEI_1 IMSI_1",
            dependsOnMethods = "editReferenceData"
    )

    public void step1() {
        commandServiceMock.run();
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_Android();
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
            description = "Провести транзакцию № 2 от имени клиента № 2 с устройства № 1 IMEI_1 IMSI_1",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(IMEI_1);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(IMSI_1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_HAS_TRANSACTIONS_NEW);
    }

    @Test(
            description = "Провести транзакцию № 3 от имени клиента № 1 с устройства № 2 IMEI_2 IMSI_2",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_Android();
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, EXIST_TRUSTED_IMSI + "\n" + EXIST_TRUSTED_IMEI + "\n");
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
