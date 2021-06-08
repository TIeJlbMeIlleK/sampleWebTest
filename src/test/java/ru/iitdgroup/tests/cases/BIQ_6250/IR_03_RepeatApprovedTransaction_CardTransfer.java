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
    private static final String PAYEE_1 = "43787" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String PAYEE_2 = "43787" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}, {"Ольга", "Петушкова", "Ильинична"}};

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final GregorianCalendar timeTable = new GregorianCalendar();
    private GregorianCalendar cloneTime;
    private GregorianCalendar cloneTime2;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Test(
            description = "Включаем правило"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
                .fillInputText("Длина серии:", "5")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод на карту другому лицу")
                .sleep(10);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
                .select("Наименование канала:", "Мобильный банк")
                .save();
    }

    @Test(
            description = "Создание клиентов и настраиваем справочники",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(dboId)
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

        timeTable.add(Calendar.MINUTE, -30);
        cloneTime2 = (GregorianCalendar) timeTable.clone();
        getIC().locateTable(PAYEE_WHITE_LIST)
                .deleteAll()
                .addRecord()
                .fillInputText("Дата занесения:", format.format(timeTable.getTime()))
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Номер карты получателя:", PAYEE_1)
                .save();
        getIC().locateTable(PAYEE_QUARANTINE_LIST)
                .deleteAll()
                .addRecord()
                .fillInputText("Дата занесения:", format.format(timeTable.getTime()))
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .fillInputText("Номер Карты получателя:", PAYEE_2)
                .save();
    }


    @Test(
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(9000));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №3 в обработку -- Получатель №1, сумма 650",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(650));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber("4378723741115555");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод на карту другому лицу» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №5 от нового клиента №2 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
        time2.add(Calendar.MINUTE, -15);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №6 от прежнего Клиента №1 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step5"
    )

    public void step6() {
        time.add(Calendar.MINUTE, 1);
        cloneTime = (GregorianCalendar) time.clone();
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №7 от прежнего Клиента №1 в обработку -- Получатель №1, сумма 500, спустя 11 минут после транзакции №6",
            dependsOnMethods = "step6"
    )

    public void step7() {
        time.add(Calendar.MINUTE, 12);
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №8 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step7"
    )

    public void step8() {
        time.add(Calendar.MINUTE, -10);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод на карту другому лицу», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №9 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step8"
    )

    public void step9() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод на карту другому лицу» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Проверить даты в справочниках карантина и доверенных получателях",
            dependsOnMethods = "step9"
    )

     public void assertTables() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // здесь формат д.б. как на экране
        getIC().locateTable(PAYEE_WHITE_LIST)
                .findRowsBy()
                .match("Номер карты получателя",PAYEE_1).click();
        assertTableField("Дата последней авторизованной транзакции:", formatter.format(cloneTime.getTime()));
        getIC().close();
    }

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
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withDestinationCardNumber(PAYEE_1)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}
