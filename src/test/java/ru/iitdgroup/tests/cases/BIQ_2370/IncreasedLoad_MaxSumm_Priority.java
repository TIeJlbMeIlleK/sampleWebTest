package ru.iitdgroup.tests.cases.BIQ_2370;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IncreasedLoad_MaxSumm_Priority extends RSHBCaseTest {

    private static final String TABLE = "(Policy_parameters) Параметры обработки событий";
    private static final String TABLE_2 = "(System_parameters) Интеграционные параметры";
    private static final String TABLE_3 = "(Rule_tables) Запрещенные получатели НомерКарты";
    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String CardNumber = "4378723741117915";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 8, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();
//    FIXME Требуется доработать автотест после исправления тикета https://yt.iitdgroup.ru/issue/BIQ2370-110

    @Test(
            description = "Включение правила"

    )
    public void enableRuleForAlert() {
        System.out.println("\"Проверка обработки транзакций при включенном признаке \"Повышенная нагрузка\"\n" +
                "Джоб RemoveSignIncreasedLoadJob снимает признак \"Повышенная нагрузка\" в справочнике \"Интеграционные параметры\"\" -- BIQ2370" + " ТК№13");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);


        getIC()
                .locateRules()
                .openRecord(RULE_NAME)
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .editBlock(0)
                .editTextField(1, 4, "30")
                .getParent()
                .save();

        //        TODO возможно данная таблица будет заполнена ранее.
        Table.Formula rows = getIC().locateTable(RDAK).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(RDAK).addRecord()
                .fillInputText("Текущий статус:","rdak_underfire")
                .fillInputText("Новый статус:","RDAK_Done")
                .save();
        getIC().locateTable(RDAK).addRecord()
                .fillInputText("Текущий статус:","Wait_RDAK")
                .fillInputText("Новый статус:","RDAK_Done")
                .save();
    }

    @Test(
            description = "Очистить справочник \"Параметры обработки событий\" от всех записей",
            dependsOnMethods = "enableRuleForAlert"
    )
    public void refactorParametres() {
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();}


        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","15000")
                .fillCheckBox("Повышенная нагрузка:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Требуется выполнение АДАК:",true)
                .fillInputText("Приоритет:", "2")
                .select("Тип транзакции:","Оплата услуг")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","15000")
                .fillCheckBox("Повышенная нагрузка:", true)
                .fillCheckBox("Требуется выполнение РДАК:", false)
                .fillCheckBox("Требуется выполнение АДАК:",false)
                .fillInputText("Приоритет:", "1")
                .select("Тип транзакции:","Оплата услуг")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","10000")
                .fillCheckBox("Повышенная нагрузка:", false)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Требуется выполнение АДАК:",true)
                .fillInputText("Приоритет:", "1")
                .select("Тип транзакции:","Оплата услуг")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

    }

    @Test(
            description = "Установить признак \"Повышенная нагрузка\"",
            dependsOnMethods = "refactorParametres"
    )
    public void enableIncreasedLoad(){
        getIC().locateTable(TABLE_2)
                .findRowsBy()
                .match("Description","Повышенная нагрузка. Если параметр включён – применяется профиль повышенной нагрузки")
                .click()
                .edit()
                .fillInputText("Значение:","1")
                .save();
    }

    @Test(
            description = "Добавить в справочник запрещенных номер карты \"4378723741117915\"",
            dependsOnMethods = "enableIncreasedLoad"
    )

    public void setBlackCard(){
        Table.Formula rows = getIC().locateTable(TABLE_3).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_3)
                .addRecord()
                .fillInputText("НомерКарты:",CardNumber)
                .save();

        getIC().locateWorkflows()
                .openRecord("Alert Workflow").openAction("Взять в работу для выполнения РДАК")
                .clearAllStates()
                .addFromState("На разбор")
                .addFromState("Ожидаю выполнения РДАК")
                .addFromState("Результат работы правил (Подозрительно)")
                .addToState("На выполнении РДАК")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "setBlackCard"
    )
    public void client() {
        try {
            for (int i = 0; i < 5; i++) {
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
            description = "Отправить транзакцию 1 \"Перевод на карту\" с DestinationCardNumber = 4378723741117915",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CardNumber);
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        getIC().locateAlerts()
                .openFirst();
        assertTableField("Severity:","Normal");

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .sleep(3);

        getIC()
                .locateRules()
                .openRecord(RULE_NAME)
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .editBlock(0)
                .editTextField(1, 4, "71")
                .getParent()
                .save();
    }

    @Test(
            description = "Отправить транзакцию 2 \"Перевод на карту\" с DestinationCardNumber = 4378723741117915",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CardNumber);
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Severity:","Major");
        assertTableField("Статус АДАК:","DISABLED");

        getIC()
                .locateRules()
                .openRecord(RULE_NAME)
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .editBlock(0)
                .editTextField(1, 4, "29")
                .getParent()
                .save();
    }

    @Test(
            description = "В справочнике \"Параметры обработки событий\" заменить в записях по каналам значения:\n" +
                    "normal --> minor\n" +
                    "major --> critical",
            dependsOnMethods = "transaction2"
    )
    public void refactorParametres_2() {
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();}


        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","5000")
                .fillInputText("Критичность транзакции:","Minor")
                .fillCheckBox("Повышенная нагрузка:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Требуется выполнение АДАК:",true)
                .fillInputText("Приоритет:", "1")
                .select("Тип транзакции:","Перевод на карту другому лицу")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","5000")
                .fillInputText("Критичность транзакции:","Critical")
                .fillCheckBox("Повышенная нагрузка:", true)
                .fillCheckBox("Требуется выполнение РДАК:", false)
                .fillInputText("Приоритет:", "1")
                .select("Тип транзакции:","Перевод на карту другому лицу")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

    }

    @Test(
            description = "Отправить транзакцию 3 \"Перевод на карту\" с DestinationCardNumber = 4378723741117915",
            dependsOnMethods = "refactorParametres_2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CardNumber);
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Severity:","Minor");

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .sleep(3);

        getIC()
                .locateRules()
                .openRecord(RULE_NAME)
                .openSpoiler("Подозрительная транзакция")
                .edit()
                .editBlock(0)
                .editTextField(1, 4, "90")
                .getParent()
                .save();
    }

    @Test(
            description = "Отправить транзакцию 4 \"Перевод на карту\" с DestinationCardNumber = 4378723741117915",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal(5001.00));
        transactionData.getCardTransfer()
                .setDestinationCardNumber(CardNumber);
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Severity:","Critical");
        assertTableField("Статус АДАК:","DISABLED");
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
}
