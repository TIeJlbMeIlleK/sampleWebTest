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
    private static final String TABLE_3 = "(Rule_tables) Запрещенные получатели НомерТелефона";
    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String TELNumber = "9299925912";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 8, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Включение правила"

    )
    public void enableRuleForAlert() {
        System.out.println("\"Проверка обработки транзакций при включенном признаке \"Повышенная нагрузка\"\n" +
                "Джоб RemoveSignIncreasedLoadJob снимает признак \"Повышенная нагрузка\" в справочнике \"Интеграционные параметры\"\" -- BIQ2370" + " ТК№27");

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
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillInputText("Приоритет:", "2")
                .select("Тип транзакции:","Оплата услуг")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","15000")
                .fillCheckBox("Повышенная нагрузка:", true)
                .fillCheckBox("Требуется выполнение РДАК:", false)
                .fillInputText("Приоритет:", "1")
                .select("Тип транзакции:","Оплата услуг")
                .select("Наименование канала ДБО:","Интернет клиент")
                .save();

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Пороговая сумма ДАК:","10000")
                .fillCheckBox("Повышенная нагрузка:", false)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Требуется выполнение АДАК:", true)
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
            description = "Добавить в справочник запрещенных номер телефона \"89299925912\"",
            dependsOnMethods = "enableIncreasedLoad"
    )

    public void setBlackCard(){
        Table.Formula rows = getIC().locateTable(TABLE_3).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE_3)
                .addRecord()
                .fillInputText("Номер телефона:",TELNumber)
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
            description = "Отправить транзакцию 1 \"Оплата услуг\" с Value = 9299925912",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal(15000.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","ENABLED");

    }

    @Test(
            description = "Отправить транзакцию 2 \"Оплата услуг\" с Value = 9299925912",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal(15001.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","ENABLED");
    }

    @Test(
            description = "Отправить транзакцию 3 \"Оплата услуг\" с Value = 9299925912",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal(14999.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","DISABLED");
    }

    @Test(
            description = "Запустить JOB RemoveSignIncreasedLoad",
            dependsOnMethods = "transaction3"
    )
    public void jobIncreasedLoad(){
        getIC().locateJobs()
                .selectJob("RemoveSignIncreasedLoadJob")
                .run();
        getIC().goToBack();
    }

    @Test(
            description = "Отправить транзакцию 4 \"Оплата услуг\" с Value = 9299925912",
            dependsOnMethods = "jobIncreasedLoad"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal(10000.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","ENABLED");
    }

    @Test(
            description = "Отправить транзакцию 5 \"Оплата услуг\" с Value = 9299925912",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getServicePayment().setAmountInSourceCurrency(new BigDecimal(9999.00));
        transactionData.getServicePayment()
                .getAdditionalField().get(0).setValue("9299925912");
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","DISABLED");

//        FIXME требуется актуализировать после исправления https://yt.iitdgroup.ru/issue/BIQ4274-30
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
