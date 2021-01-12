package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_01_CheckDBO extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    //private GregorianCalendar time2;
    //private GregorianCalendar time3;
    private HashMap<String, Object> map = new HashMap<>();
    private HashMap<String, Object> map1 = new HashMap<>();
    //private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    //private final DateFormat format2 = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Кука", "Пашкин", "Семенович"}};

    private static final String RULE_NAME = "R01_IR_01_CheckDBO";
    private static final String REFERENCE_ITEM1 = "(System_tables) Типы транзакций";
    private static final String REFERENCE_ITEM2 = "(System_tables) Каналы ДБО";
    private static final String REFERENCE_ITEM3 = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String TYPE_TSP1 = new RandomString(8).nextString();
    private static final String TYPE_TSP2 = new RandomString(8).nextString();


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
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
            description = "1. В справочник \"Типы транзакци\" заведены - \"Платеж по QR-коду через СБП\"" +
                    "2. В справочник \"Каналы ДБО\" заведены каналы INTERNET_CLIENT и MOBILE_BANK" +
                    "3. В справочник \"Проверяемые типы транзакций и каналы ДБО\" занести запись для \"Интернет банк\" " +
                    "и \"Платеж по QR-коду через СБП\"" +
                    "Включено правило IR_01_CheckDBO",
            dependsOnMethods = "addClient"
    )
    public void makeChangesToTheDirectory() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);

        Table table = getIC().locateTable(REFERENCE_ITEM1);
        int count = table
                .findRowsBy()
                .match("Наименование типа транзакции", "Платеж по QR-коду через СБП")
                .countMatchedRows();

        if (count == 0) {//если записи отсутствуют, заносим новые
            table.addRecord().fillInputText("Наименование типа транзакции:", "Платеж по QR-коду через СБП")
                    .fillInputText("Системное название:", "PAYMENT_C2B").save();
        }

        Table table2 = getIC().locateTable(REFERENCE_ITEM2);
        int count2 = table2
                .findRowsBy()
                .match("Наименование канала ДБО", "Интернет клиент")
                .countMatchedRows();

        if (count2 == 0) {//если записи отсутствуют, заносим новые
            table.addRecord().fillInputText("Системное название:", "MOBILE_BANK")
                    .fillInputText("Наименование канала ДБО:", "Мобильный банк").save();
        }

        Table table3 = getIC().locateTable(REFERENCE_ITEM2);
        int count3 = table3
                .findRowsBy()
                .match("Наименование канала ДБО", "Мобильный банк")
                .countMatchedRows();

        if (count3 == 0){
            table.addRecord().fillInputText("Наименование канала ДБО:", "Интернет клиент")
                    .fillInputText("Системное название:", "INTERNET_CLIENT").save();
        }

        getIC().locateTable(REFERENCE_ITEM3)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .select("Наименование канала:", "Интернет клиент")
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 1 из \"Мобильный банк\" типа \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "makeChangesToTheDirectory"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Непроверяемая пара канал ДБО - тип транзакции");
    }

    @Test(
            description = " Провести транзакцию № 2 из \"Интернет банк\" типа \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransactionPC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TYPE_TSP2)
                .withTSPType(TYPE_TSP2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Проверяемая пара канал ДБО - тип транзакции");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPC() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
