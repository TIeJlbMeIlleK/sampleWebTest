package ru.iitdgroup.tests.cases.BIQ_6250;

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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_03_RepeatApprovedTransaction_CardTransfer extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String PAYEE_WHITE_LIST = "(Rule_tables) Доверенные получатели";
    private static final String PAYEE_QUARANTINE_LIST = "(Rule_tables) Карантин получателей";
    private static final String PAYEE_1 = "4378723741117777";
    private static final String PAYEE_2 = "4378723741115555";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final GregorianCalendar time_new = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 5; i++) {
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило и выполняем преднастройки",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Длина серии:","3")
                .fillInputText("Период серии в минутах:","10")
                .fillCheckBox("РДАК выполнен:",false)
                .fillCheckBox("АДАК выполнен:",false)
                .fillCheckBox("Требовать совпадения остатка на счете:",false)
                .select("Тип транзакции:","CARD_TRANSFER")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);

        Table.Formula rows = getIC().locateTable(PAYEE_WHITE_LIST).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(PAYEE_WHITE_LIST)
                .addRecord()
                .fillUser("ФИО Клиента:",clientIds.get(0))
                .fillInputText("Номер карты получателя:",PAYEE_1)
                .save();

        Table.Formula rows1 = getIC().locateTable(PAYEE_QUARANTINE_LIST).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(PAYEE_QUARANTINE_LIST)
                .addRecord()
                .fillUser("ФИО Клиента:",clientIds.get(1))
                .fillInputText("Номер Карты получателя:",PAYEE_2)
                .save();
        commandServiceMock.run();
    }

    @Test(
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
    }

    @Test(
            description = "Отправить Транзакцию №3 в обработку -- Получатель №1, сумма 501",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(501));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber("4378723741115555");
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №5 от нового клиента №2 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №6 от прежнего Клиента №1 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step5"
    )

    public void step6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
    }

    @Test(
            description = "Отправить Транзакцию №7 от прежнего Клиента №1 в обработку -- Получатель №1, сумма 500, спустя 11 минут после транзакции №7",
            dependsOnMethods = "step6"
    )

    public void step7() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №8 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step7"
    )

    public void step8() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_2);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RULE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №9 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step8"
    )

    public void step9() {
        time_new.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber(PAYEE_2);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
    }

    @Test(
            description = "Проверить даты в справочниках карантина и доверенных получателях",
            dependsOnMethods = "step9"
    )

    public void assertTables() {
        getIC().locateTable(PAYEE_WHITE_LIST)
                .findRowsBy()
                .match("Номер карты получателя",PAYEE_1).click();
        assertTableField("Дата последней авторизованной транзакции:", "01.11.2020 00:05:00");

//        SimpleDateFormat formatter = new SimpleDateFormat("dd.mm.yyyy HH:mm:ss"); // здесь формат д.б. как на экране
//        getIC().locateTable(PAYEE_WHITE_LIST)
//                .findRowsBy()
//                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером",PAYEE_1).click();
//        assertTableField("Дата последней авторизованной транзакции:", formatter.format(time.getTime()));

        //TODO раскоменнтировать после исправления ошибки обновления даты последней транзакции в карантике получателей
//        getIC().locateTable(PAYEE_QUARANTINE_LIST)
//                .findRowsBy()
//                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером",PAYEE_2).click();
//        assertTableField("Дата последней авторизованной транзакции:", formatter.format(time_new.getTime()));
        getIC().close();
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "assertTables"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
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
