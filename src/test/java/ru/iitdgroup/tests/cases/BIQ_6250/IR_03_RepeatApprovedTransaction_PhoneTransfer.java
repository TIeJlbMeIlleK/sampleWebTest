package ru.iitdgroup.tests.cases.BIQ_6250;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransaction_PhoneTransfer extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String PAYEE_WHITE_LIST = "(Rule_tables) Доверенные получатели";
    private static final String PAYEE_QUARANTINE_LIST = "(Rule_tables) Карантин получателей";
    private static final String PHONE_1 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String PHONE_2 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String PAYEE_1 = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 11);

    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final GregorianCalendar time_new = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Леонид", "Жуков", "Игоревич"}, {"Петр", "Серебряков", "Иванович"}};

    @Test(
            description = "Включаем правило и выполняем преднастройки"
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
                .fillInputText("Длина серии:", "3")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachTransactionIR03("Типы транзакций", "Перевод по номеру телефона")
                .sleep(10);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод по номеру телефона")
                .select("Наименование канала:", "Мобильный банк")
                .save();

        commandServiceMock.run();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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

        getIC().locateTable(PAYEE_WHITE_LIST)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:",clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", PHONE_1)
                .save();

        getIC().locateTable(PAYEE_QUARANTINE_LIST)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:",clientIds.get(1))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", PHONE_2)
                .save();
    }

    @Test(
            description = "Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №3 в обработку -- Получатель №1, сумма 800",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №4 в обработку -- новый номер телефона, название банка прежнее, сумма 500",
            dependsOnMethods = "step3"
    )

    public void step4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone("79469634578");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №5 в обработку -- изменить наименование Банка, номер телефона старый (Получатель №1), сумма 500",
            dependsOnMethods = "step4"
    )

    public void step5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withBankName("ВЭБ Банк");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №7 от прежнего клиента №1 в обработку -- Получатель №1, сумма 500",
            dependsOnMethods = "step5"
    )

    public void step6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №8 от прежнего клиента №1 в обработку -- Получатель №1, сумма 500, спустя 11 минут",
            dependsOnMethods = "step6"
    )

    public void step7() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_new))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_new));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод по номеру телефона» условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №9 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step7"
    )

    public void step8() {
        time_new.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_new));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(PHONE_2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод по номеру телефона», условия правила не выполнены");
    }

    @Test(
            description = "Отправить Транзакцию №10 от Клиента №3 в обработку -- Получатель №2, сумма 500",
            dependsOnMethods = "step8"
    )

    public void step9() {
        time_new.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_new));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PHONE_2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод по номеру телефона» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить Транзакцию №11 и 12 Запрос на выдачу кредита и Перевод на карту",
            dependsOnMethods = "step9"
    )

    public void step10() {
        time_new.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");

        Transaction transactionServicePayment = getTransactionServicePayment();
        sendAndAssert(transactionServicePayment);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Непроверяемый тип транзакции");
    }

    @Test(
            description = "Проверить даты в справочниках карантина и доверенных получателях",
            dependsOnMethods = "step9"
    )

    public void assertTables() {
        getIC().locateTable(PAYEE_WHITE_LIST)
                .findRowsBy()
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", PHONE_1).click();
        assertTableField("Дата последней авторизованной транзакции:", "01.11.2020 00:05:00");
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

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getPhoneNumberTransfer()
                .withPayeePhone(PHONE_1);
        transaction.getData().getTransactionData()
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .withBankName("РОССЕЛЬХОЗБАНК");
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_new))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_new))
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500))
                .withDestinationCardNumber(PAYEE_1);
        return transaction;
    }

    private Transaction getTransactionServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_new))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_new));
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}
