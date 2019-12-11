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

public class IR_01_CheckDBO extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_01_CheckDBO";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
//        Перед прохождение авто-теста убедиться в наличии записей как в преднастройках  В справочник "Типы транзакци" заведены
//1 - Открытие вклада
//2 - Закрытие вклада
//3 - Открытие текущего счета (в т.ч. накопительного)
//4 - Закрытие текущего счета (в т.ч. накопительного)
//5 - Перевод между счетами
//6 - Перевод на счет другому лицу
//7 - Перевод на карту другому лицу
//8 - Перевод в бюджет, оплата начислений, получаемых из ГИС ГМП
//9 - Оплата услуг
//10 - Перевод через систему денежных переводов
//11 - Изменение перевода, отправленного через систему денежных переводов
//12 - Перевод со сторонней карты на карту банка
//
//2. В справочник "Каналы ДБО" заведены каналы INTERNET_CLIENT и MOBILE_BANK
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "В справочник \"Каналы ДБО\" заведены каналы INTERNET_CLIENT и MOBILE_BANK\n" +
                    "3. В справочник \"Проверяемые типы транзакций и каналы ДБО\" занести запись для \"Интернет банк\" и \"Перевод на карту\"",
            dependsOnMethods = "enableRules"
    )
    public void editTable() {

        Table.Formula rows = getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Перевод на карту другому лицу")
                .select("Наименование канала:","Интернет клиент")
                .save();

        getIC().locateTable("(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО")
                .addRecord()
                .select("Тип транзакции:","Перевод в сторону государства")
                .select("Наименование канала:","Мобильный банк")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editTable"
    )
    public void client() {
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
            description = "Провести транзакцию № 1 из \"Интернет банк\" типа \"Перевод на карту\"",
            dependsOnMethods = "client"
    )
    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, FALSE_EX_IR1);
    }

    @Test(
            description = "Провести транзакцию № 2 из \"Мобильный банк\" типа \"Перевод на карту\"",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER_MOBILE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EX_IR1);
    }

    @Test(
            description = "Провести транзакцию № 3 из \"Интернет банк\" типа \"Перевод в бюджет\"",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionBUDGET_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EX_IR1);
    }

    @Test(
            description = "Провести транзакцию № 4 из \"Мобильный банк\" типа \"Перевод в бюджет\"",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionBUDGET_TRANSFER_MOBILE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, FALSE_EX_IR1);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER_MOBILE() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionBUDGET_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionBUDGET_TRANSFER_MOBILE() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
