package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class R01_GR_03_SeriesOneToMany_RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private static final String RULE_NAME_2 = "R01_ExR_05_GrayIP";
    private static final String IP = "192.168.5.1";
    private static final String GREY_IP = "(Rule_tables) Подозрительные IP адреса";
    private static final String TABLE_PARAMETRES = "(Policy_parameters) Параметры обработки событий";
    private static final String TABLE_INTEGRO = "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.SEPTEMBER, 11, 10, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("\"Правило GR_03 не учитывает в работе транзакции, подтвержденные по РДАК\" -- BIQ2370" + " ТК№13(103)");

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:","60")
                .fillInputText("Длина серии:","3")
                .fillInputText("Сумма серии:","2000")
                .save()
                .sleep(5);
        getIC().locateRules()
                .editRule(RULE_NAME_2)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Настроить WF для попадания первой транзакции на РДАК",
            dependsOnMethods = "enableRules"
    )
    public void refactorWF(){
        getIC().locateWorkflows()
                .openRecord("Alert Workflow").openAction("Взять в работу для выполнения РДАК")
                .clearAllStates()
                .addFromState("На разбор")
                .addFromState("Ожидаю выполнения РДАК")
                .addToState("На выполнении РДАК")
                .save();

        Table.Formula rows = getIC().locateTable(RDAK).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","rdak_underfire")
                .fillInputText("Новый статус:","RDAK_Done").save();
        getIC().locateTable(RDAK).addRecord().fillInputText("Текущий статус:","Wait_RDAK")
                .fillInputText("Новый статус:","RDAK_Done").save();

        Table.Formula rows2 = getIC().locateTable(TABLE_PARAMETRES).findRowsBy();
        if (rows2.calcMatchedRows().getTableRowNums().size() > 0) {
            rows2.delete();
        }
        getIC().locateTable(TABLE_PARAMETRES)
                .addRecord()
                .select("Наименование канала ДБО:","Интернет клиент")
                .select("Тип транзакции:","Перевод на карту другому лицу")
                .fillCheckBox("Требуется выполнение РДАК:",true)
                .save();

        getIC().locateTable(TABLE_INTEGRO)
                .findRowsBy()
                .match("Description","Повышенная нагрузка. Если параметр включён – применяется профиль повышенной нагрузки")
                .edit()
                .fillInputText("Значение:","0")
                .save();

        Table.Formula rows3 = getIC().locateTable(GREY_IP).findRowsBy();
        if (rows3.calcMatchedRows().getTableRowNums().size() > 0) {
            rows3.delete();
        }
        getIC().locateTable(GREY_IP)
                .addRecord()
                .fillInputText("IP устройства:",IP)
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "refactorWF"
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
            description = "Провести транзакцию № 1 \"Перевод на карту\" для клиента № 1, сумма 50",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getCardTransfer().setAmountInSourceCurrency(new BigDecimal(50));
        transactionData.getClientDevice().getPC().setIpAddress(IP);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .sleep(3)
                .rdak()
                .fillCheckBox("Верный ответ",true)
                .getDriver().findElement(By.id("_ic_rdak_btn_ok")).click();
        getIC().close();
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(
            description = "Провести транзакцию № 2 \"Перевод через систему денежных переводов\" для Клиента № 1, сумма 50",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer().setAmountInSourceCurrency(new BigDecimal(51.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на счет\" для Клиента № 1, сумма 900",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer().setAmountInSourceCurrency(new BigDecimal(900));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
