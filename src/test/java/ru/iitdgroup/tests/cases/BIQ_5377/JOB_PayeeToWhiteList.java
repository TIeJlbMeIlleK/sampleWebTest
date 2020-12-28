package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class JOB_PayeeToWhiteList extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time2;
    private GregorianCalendar time3;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private Client client = null;

    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Карантин получателей";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";
    private static final String TYPE_TSP = new RandomString(8).nextString();
    private static final String TYPE_TSP1 = new RandomString(8).nextString();


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName("Мила")
                        .withLastName("Зыкина")
                        .withMiddleName("Олеговна")
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                this.client = client;
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включить и настроить правило GR_20 и " +
                    "Очистить справочник «Карантин Получателей» и «Доверенные Получатели»\"" +
                    "\"Установить CLAIM_PERIOD = 1  (справочник \"Параметры обработки справочников и флагов\")",
            dependsOnMethods = "addClient"
    )
    public void enableRules() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM1).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {//очищает карантин
            rows.selectLinesAndDelete();
        }

        Table.Formula rows1 = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {//очищает доверенных
            rows1.selectLinesAndDelete();
        }

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy().match("код значения", "CLAIM_PERIOD").click()
                .edit().fillInputText("Значение:", "1").save().sleep(1);

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);
    }

    @Test(
            description = " Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1" +
                    "и проверить справочник Карантин Получателей",
            dependsOnMethods = "enableRules"
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);

        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();

        getIC().locateTable(REFERENCE_ITEM1)
                .refreshTable()
                .findRowsBy()
                .match("Дата последней авторизованной транзакции", format.format(time.getTime()))
                .match("Имя получателя", TYPE_TSP)
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи
    }

    @Test(
            description = "Изменить в БД \"Дата занесения в карантин\" для транзакций №1 на дату ранее 1 дня назад.",
            dependsOnMethods = "step1"
    )
    public void changeDateInDatabase() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("TIME_STAMP", Instant.now().minus(2, ChronoUnit.DAYS).toString());
        map.put("LAST_TRANSACTION", Instant.now().minus(2, ChronoUnit.DAYS).toString());
        getDatabase().updateWhere("QUARANTINE_LIST", map, "WHERE [id] = (SELECT MAX([id]) FROM [QUARANTINE_LIST])");
    }

    @Test(
            description = " Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №2",
            dependsOnMethods = "changeDateInDatabase"
    )

    public void step2() {
        time.add(Calendar.MINUTE, 1);
        time2 = (GregorianCalendar) time.clone();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP1)
                .withTSPType(TYPE_TSP1);
        sendAndAssert(transaction);

        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();

        getIC().locateTable(REFERENCE_ITEM1)
                .refreshTable()
                .findRowsBy()
                .match("Дата последней авторизованной транзакции", format.format(time2.getTime()))
                .match("Имя получателя", TYPE_TSP1)
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи
    }

    @Test(
            description = "Запустить джоб JOB PayeeToWhiteList",
            dependsOnMethods = "step2"
    )

    public void runJobStep() {

        getIC().locateJobs()
                .selectJob("PayeeToWhiteList")
                .run()
                .waitStatus(JobRunEdit.JobStatus.SUCCESS);

        getIC().home();
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1",
            dependsOnMethods = "runJobStep"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        time3 = (GregorianCalendar) time.clone();
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time3))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time3))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        sendAndAssert(transaction);
    }

    @Test(
            description = "Проверить \"Доверенные получатели\" и" +
                    "\"Карантин получателей\"",
            dependsOnMethods = "step3"
    )

    public void checkingReferenceBooks() {
        String name = client.getData().getClientData().getClient().getLastName() + ' ' +
                client.getData().getClientData().getClient().getFirstName() + ' ' +
                client.getData().getClientData().getClient().getMiddleName();

        getIC().locateTable(REFERENCE_ITEM2)//проверка доверенных
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP)
                .match("Дата последней авторизованной транзакции", format.format(time3.getTime()))
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM1)//проверка карантина
                .refreshTable()
                .findRowsBy()
                .match("Дата последней авторизованной транзакции", format.format(time2.getTime()))
                .match("Имя получателя", TYPE_TSP1)
                .match("ФИО Клиента", name)
                .failIfNoRows(); //проверка справочника на наличие записи
        getIC().close();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
