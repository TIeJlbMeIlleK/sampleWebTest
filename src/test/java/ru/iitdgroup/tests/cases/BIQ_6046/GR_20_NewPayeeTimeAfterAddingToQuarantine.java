package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import java.text.DateFormat;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_20_NewPayeeTimeAfterAddingToQuarantine extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();


    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Семен", "Скирин", "Федорович"}};
    private static String[] login = {new RandomString(5).nextString(), new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Карантин получателей";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";
    private static final String REFERENCE_ITEM3 = "(Policy_parameters) Параметры обработки справочников и флагов";

    private static final String TRUSTED_RECIPIENT = "Петр Ильич Филин";
    private static final String QUARANTINE_RECIPIENT = "Олеся Викторовна Зимина";
    private static final String QUARANTINE_RECIPIENT1 = "Олег Дмитриевич Сидоренко";
    private static final String BIK1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String BIK2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String BIK_TRUSTED = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String INN1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String INN2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String INN_TRUSTED = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String ACCOUNT1 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private static final String ACCOUNT2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
    private static final String ACCOUNT_TRUSTED = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withLogin(login[i])
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(loginHash[i])
                        .withDboId(dboId[i])
                        .withCifId(dboId[i])
                        .withExpertSystemId(dboId[i])
                        .withEksId(dboId[i])
                        .getAlfaIds()
                        .withAlfaId(dboId[i]);

                sendAndAssert(client);
                clientIds.add(dboId[i]);
                System.out.println(dboId[i]);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включить правило GR_20_NewPayee." +
                    "Занести получателя №1 в карантин для клиента № 1 (Дата занесения меньше TIME_AFTER_ADDING_TO_QUARANTINE)" +
                    "Занести получателя №2 в карантин для клиента № 1 (Дата занесения больше TIME_AFTER_ADDING_TO_QUARANTINE)" +
                    "Занести получателя №3 в доверенные для клиента №1",
            dependsOnMethods = "addClient"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM3)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();

//TODO перед запуском в справочнике Карантин получателей должны быть поля "Дата занесения"

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM1).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        time1.add(Calendar.HOUR, -10);
        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Имя получателя:", QUARANTINE_RECIPIENT)
                .fillInputText("Дата занесения:", format.format(time1.getTime()))
                .fillInputText("БИК банка получателя:", BIK1)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT1)
                .fillInputText("ИНН получателя:", INN1)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();
        time2.add(Calendar.HOUR, -28);
        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Дата занесения:", format.format(time2.getTime()))
                .fillInputText("Имя получателя:", QUARANTINE_RECIPIENT1)
                .fillInputText("БИК банка получателя:", BIK2)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT2)
                .fillInputText("ИНН получателя:", INN2)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();

        Table.Formula rows1 = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }

        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillInputText("Дата занесения:", format.format(time.getTime()))
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT)
                .fillInputText("БИК банка получателя:", BIK_TRUSTED)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT_TRUSTED)
                .fillInputText("ИНН получателя:", INN_TRUSTED)
                .save();
    }

    @Test(
            description = "Провести транзакцию № 1 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя №1, находящегося в карантине (БИК+СЧЕТ)",
            dependsOnMethods = "enableRules"
    )

    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(ACCOUNT1)
                .withPayeeINN(null)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(BIK1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя №2, находящегося в карантине (БИК+СЧЕТ)",
            dependsOnMethods = "transaction1"
    )

    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(ACCOUNT2)
                .withPayeeINN(null)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(BIK2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя №1, находящегося в карантине (ИНН)",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(null)
                .withPayeeINN(INN1)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя №2, находящегося в карантине (ИНН)",
            dependsOnMethods = "transaction3"
    )

    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(null)
                .withPayeeINN(INN2)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION);
    }

    @Test(
            description = "Провести транзакцию № 5 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя, находящегося в доверенных (БИК+СЧЕТ)",
            dependsOnMethods = "transaction4"
    )

    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(ACCOUNT_TRUSTED)
                .withPayeeINN(null)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(BIK_TRUSTED);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Получатель найден в списке разрешенных");
    }

    @Test(
            description = "Провести транзакцию № 6 \"Перевод на счет другому лицу\" от имени клиента № 1 в пользу получателя, находящегося в доверенных (ИНН)",
            dependsOnMethods = "transaction5"
    )

    public void transaction6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(null)
                .withPayeeINN(INN_TRUSTED)
                .withPayeeName(null);
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Получатель найден в списке разрешенных");
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
