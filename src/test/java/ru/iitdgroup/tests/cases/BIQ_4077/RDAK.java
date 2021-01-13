package ru.iitdgroup.tests.cases.BIQ_4077;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Performances_RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Вопросы для проведения ДАК";

    private static Random rand = new Random();

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time2;

    private final List<String> clientIds = new ArrayList<>();
    private Client client = null;

//TODO для прохождения теста в Alert должны быть внесены поля:Идентификатор клиента, Status (Алерта), Статус РДАК, status(транзакции)

    @Test(
            description = "Занести транзакция в проверяемые, " +
                    "в справочнике событий выбрать клиентов по умолчанию и применить РДАК. " +
                    "Выбрать правило GR_20." +
                    "Заполнить \"Вопросы для проведения ДАК\": " +
                    "registrationHouse, registrationStreet, birthDate с установленными флагами \"Включено\" и \"Учавствует в РДАК\""
    )
    public void enableRules() {

        getIC().locateRules()
                .selectVisible()
                .deactivate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "По умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .select("Наименование канала ДБО:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .setTableFilter("Текст вопроса клиенту", "Equals", "Ваше имя")
                .refreshTable()
                .click(2)
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true).save().sleep(2);
        getIC().locateTable(REFERENCE_ITEM2)
                .setTableFilter("Текст вопроса клиенту", "Equals", "Дата вашего рождения полностью")
                .refreshTable()
                .click(2)
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true).save().sleep(2);
    }

    @Test(
            description = "Настроить WF для попадания первой транзакции на РДАК и" +
                    "Заполнить справочник \"Перечень статусов для которых применять РДАК\" из rdak_underfire в RDAK_Done",
            dependsOnMethods = "enableRules"
    )
    public void refactorWF() {

        getIC().locateWorkflows()
                .openRecord("Alert Workflow")
                .openAction("Взять в работу для выполнения РДАК")
                .clearAllStates()
                .addFromState("На разбор")
                .addFromState("Ожидаю выполнения РДАК")
                .addToState("На выполнении РДАК")
                .save();

//Заполнение справочника "Перечень статусов для которых применять РДАК" из rdak_underfire в RDAK_Done: 2 варианта
        //1й вариант через браузер:
//        Table.Formula rows = getIC().locateTable(RDAK).findRowsBy();
//        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
//            rows.delete();
//        }
//        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","rdak_underfire")
//                .fillInputText("Новый статус:","RDAK_Done").save();
//        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","Wait_RDAK")
//                .fillInputText("Новый статус:","RDAK_Done").save();

        //2й вариант через БД:
        getDatabase().deleteWhere("LIST_APPLY_RDAKSTATUS", "");
        getDatabase().insertRows("LIST_APPLY_RDAKSTATUS", new String[]{"'rdak_underfire', 'RDAK_Done'", "'Wait_RDAK', 'RDAK_Done'"});
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "refactorWF"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName("Наталья")
                        .withLastName("Сколкина")
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
            description = "Провести транзакции № 1",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .notMyPayment()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "WRONG");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .clientNotConfirmed()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "NOT_CONFIRMED_CLIENT");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 3",
            dependsOnMethods = "transaction1"
    )
    public void transaction3() {
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .theClientWillCallHimself()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "На выполнении РДАК");
        assertTableField("Статус РДАК:", "CLIENT_CALL");
        assertTableField("status:", "Подозрительная");

        getIC().locateAlerts()
                .openFirst()
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .NotKnown()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "UNKNOWN");
        assertTableField("status:", "Подозрительная");
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
