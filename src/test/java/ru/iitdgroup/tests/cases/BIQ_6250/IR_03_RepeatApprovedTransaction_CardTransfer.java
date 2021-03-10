package ru.iitdgroup.tests.cases.BIQ_6250;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static String TRANSACTION_ID;
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}, {"Ольга", "Петушкова", "Ильинична"}};
    private static final String LOGIN_1 = new RandomString(5).nextString();
    private static final String LOGIN_2 = new RandomString(5).nextString();
    private static final String LOGIN_3 = new RandomString(5).nextString();
    private static final String LOGIN_HASH1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN_HASH2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN_HASH3 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.MARCH, 1, 0, 0, 0);
    private final GregorianCalendar time_new = new GregorianCalendar(2020, Calendar.MARCH, 1, 0, 0, 0);
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");


    @Test(
            description = "Создание клиентов"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                Client client = new Client("testCases/Templates/client.xml");
                if (i == 0) {
                    client.getData().getClientData().getClient().withLogin(LOGIN_1);
                } else if (i == 1) {
                    client.getData().getClientData().getClient().withLogin(LOGIN_2);
                } else {
                    client.getData().getClientData().getClient().withLogin(LOGIN_3);
                }

                if (i == 0) {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH1);
                } else if (i == 1) {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH2);
                } else {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH3);
                }
                client.getData()
                        .getClientData()
                        .getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);

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
            dependsOnMethods = "addClient"

    )
    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .editRule(RULE_NAME)
//                .fillCheckBox("Active:", true)
//                .fillCheckBox("АДАК выполнен:", false)
//                .fillCheckBox("РДАК выполнен:", false)
//                .fillCheckBox("Требовать совпадения остатка на счете:", false)
//                .fillInputText("Длина серии:", "3")
//                .fillInputText("Период серии в минутах:", "10")
//                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
//                .save()
//                .detachWithoutRecording("Типы транзакций")
//                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
//                .sleep(10);
//
//        Table.Formula rows = getIC().locateTable(REFERENCE_TABLE).findRowsBy();
//        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
//            rows.delete();
//        }
//        getIC().locateTable(REFERENCE_TABLE)
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();

        Table.Formula rows1 = getIC().locateTable(PAYEE_WHITE_LIST).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(PAYEE_WHITE_LIST)
                .addRecord()
                .fillInputText("Дата занесения:", format.format(time.getTime()))
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Номер карты получателя:", PAYEE_1)
                .save();


        Table.Formula rows2 = getIC().locateTable(PAYEE_QUARANTINE_LIST).findRowsBy();
        if (rows2.calcMatchedRows().getTableRowNums().size() > 0) {
            rows2.delete();
        }
        getIC().locateTable(PAYEE_QUARANTINE_LIST)
                .addRecord()
                .fillInputText("Дата занесения:", format.format(time.getTime()))
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .fillInputText("Номер Карты получателя:", PAYEE_2)
                .save();
    }

    @Test(
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "enableRules"
    )

    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №3 в обработку -- Получатель №1, сумма 650",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(650));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber("4378723741115555")
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №5 от нового клиента №2 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
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
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
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
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
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
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_2)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
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
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_2)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

//    @Test(
//            description = "Проверить даты в справочниках карантина и доверенных получателях",
//            dependsOnMethods = "step9"
//    )

    // public void assertTables() {
//        getIC().locateTable(PAYEE_WHITE_LIST)
//                .findRowsBy()
//                .match("Номер карты получателя",PAYEE_1).click();
//        assertTableField("Дата последней авторизованной транзакции:", "01.03.2020 00:00:00");

//        SimpleDateFormat formatter = new SimpleDateFormat("dd.mm.yyyy HH:mm:ss"); // здесь формат д.б. как на экране
//        getIC().locateTable(PAYEE_WHITE_LIST)
//                .findRowsBy()
//                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером",PAYEE_1).click();
//        assertTableField("Дата последней авторизованной транзакции:", formatter.format(time.getTime()));

    //TODO раскоменнтировать после исправления ошибки обновления даты последней транзакции в карантине получателей

//        getIC().locateTable(PAYEE_QUARANTINE_LIST)
//                .findRowsBy()
//                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером",PAYEE_2).click();
//        assertTableField("Дата последней авторизованной транзакции:", formatter.format(time_new.getTime()));
//        getIC().close();
//    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
