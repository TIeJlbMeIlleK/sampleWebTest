package ru.iitdgroup.tests.cases.BIQ_8994;

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

public class All_White_Rules extends RSHBCaseTest {

    private static final String RULE_NAME_WR_01 = "R01_WR_01_OwnAccount";
    private static final String RULE_NAME_WR_02 = "R01_WR_02_BudgetTransfer";
    private static final String RULE_NAME_WR_03 = "R01_WR_03_EntrustedTransfer";
    private static final String RULE_NAME_WR_04 = "R01_WR_04_ToVip";
    private static final String RULE_NAME_WR_05 = "R01_WR_05_TransferOnOldAcc";
    private static final String RULE_NAME_WR_06 = "R01_WR_06_VES";
    private static final String RULE_NAME_WR_07 = "R01_WR_07_TransferToOtherBankOwnAccount";
    private static final String RULE_NAME_WR_08 = "R01_WR_08_EntrustedAccMask";
    private static final String RULE_NAME_WR_09 = "R01_WR_09_NonRiskTSP";
    private static final String RULE_NAME_ExR_01 = "R01_ExR_01_AuthenticationContactChanged";
    private static final String RULE_NAME_ExR_02 = "R01_ExR_02_AnomalGeoPosChange";
    private static final String RULE_NAME_ExR_05 = "R01_ExR_05_GrayIP";
    private static final String RULE_NAME_ExR_06 = "R01_ExR_06_GrayDevice";
    private static final String RULE_NAME_IR_01 = "R01_IR_01_CheckDBO";
    private static final String RULE_NAME_BR_01 = "R01_BR_01_PayeeInBlackList";
    private static final String TABLE_TYPE_TRANSACTION = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String TABLE_BLACK_RECIPIENTS_PHONE_NUMBER = "(Rule_tables) Запрещенные получатели НомерТелефона";
    private static final String TABLE_TRUSTED_RECIPIENTS = "(Rule_tables) Доверенные получатели";
    private static final String TABLE_SUSPICIOUS_IP = "(Rule_tables) Подозрительные IP адреса";
    private static final String GREY_IP = "215.198.10.1";
    private static final String BLACK_PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String MASK = "427" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 3);
    private static final String CARD = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String CARD_TRUSTED = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String ACCOUNT = "105634" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String INN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String TRUSTED_PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String SERVICE = "МТС мобильная связь";
    private static final String PROVIDER = "МТС";
    private final String[][] names = {{"Анастасия", "Смирнова", "Витальевна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "1. Включить правила: IR_01, BR_01, EXR_01: 02, 05, 06, WR_01: 02, 03, 04, 05, 06, 07, 08, 09." +
                    "2. WR_01 (Белые правила): В параметр добавлены правила Исключения:" +
                    "- ExR_01_AuthenticationContactChanged, ExR_02_AnomalGeoPosChange" +
                    "WR_03 В параметр добавлены правила Исключения: ExR_06_GrayDevice " +
                    "WR_06: Крупный перевод: 5000, Код ответа ВЭС: 46" +
                    "WR_08 В параметр добавлены правила Исключения:: ExR_05_GrayIP (Правило Исключение)"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_IR_01)
                .selectRule(RULE_NAME_BR_01)
                .selectRule(RULE_NAME_ExR_01)
                .selectRule(RULE_NAME_ExR_02)
                .selectRule(RULE_NAME_ExR_05)
                .selectRule(RULE_NAME_ExR_06)
                .activate()
                .editRule(RULE_NAME_WR_01)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .attachPersonalExceptions("ExR_01_AuthenticationContactChanged")
                .attachPersonalExceptions("ExR_02_AnomalGeoPosChange")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_02)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_07)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_04)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_09)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_03)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .attachPersonalExceptions("ExR_06_GrayDevice")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_05)
                .fillCheckBox("Active:", true)
                .fillInputText("Период в днях для «старого» счёта:", "10")
                .save()
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_06)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000,00")
                .save()
                .detachWithoutRecording("Коды ответов ВЭС")
                .attachVESCode46("Коды ответов ВЭС")
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_08)
                .fillCheckBox("Active:", true)
                .save()
                .detachAll("Маски счетов")
                .addAttachMask("Маски счетов доверенных получателей", MASK)
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .attachPersonalExceptions("ExR_05_GrayIP")
                .sleep(5);

        getIC().locateTable(TABLE_SUSPICIOUS_IP)
                .deleteAll()
                .addRecord()
                .fillInputText("IP устройства:", GREY_IP)
                .save();

        getIC().locateTable(TABLE_BLACK_RECIPIENTS_PHONE_NUMBER)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер телефона:", BLACK_PHONE)
                .save();

        Table rows = getIC().locateTable(TABLE_TYPE_TRANSACTION)
                .setTableFilter("Тип транзакции", "Equals", "Оплата услуг").refreshTable();
        if (getIC().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            rows.addRecord()
                    .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Оплата услуг")
                    .select("Наименование канала:", "Мобильный банк").save();
        }

        Table rows1 = getIC().locateTable(TABLE_TYPE_TRANSACTION)
                .setTableFilter("Тип транзакции", "Equals", "Перевод в сторону государства").refreshTable();
        if (getIC().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            rows1.addRecord()
                    .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод в сторону государства")
                    .select("Наименование канала:", "Мобильный банк").save();
        }

        Table rows2 = getIC().locateTable(TABLE_TYPE_TRANSACTION)
                .setTableFilter("Тип транзакции", "Equals", "Запрос на выдачу кредита").refreshTable();
        if (getIC().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            rows2.addRecord()
                    .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
                    .select("Наименование канала:", "Мобильный банк").save();
        }

        Table rows3 = getIC().locateTable(TABLE_TYPE_TRANSACTION)
                .setTableFilter("Тип транзакции", "Equals", "Перевод между счетами").refreshTable();
        if (getIC().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            rows3.addRecord()
                    .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод между счетами")
                    .select("Наименование канала:", "Мобильный банк").save();
        }
    }

    @Test(
            description = "Создаем клиента" +
                    "От Клиента №1 в справочник \"Доверенные получатели\" внесен доверенный получатель",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
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

        getIC().locateTable(TABLE_TRUSTED_RECIPIENTS)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", "Сергей")
                .fillInputText("Номер карты получателя:", CARD_TRUSTED)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT)
                .fillInputText("БИК банка получателя:", BIK)
                .fillInputText("ИНН получателя:", INN)
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", TRUSTED_PHONE)
                .fillInputText("Наименование сервиса:", SERVICE)
                .fillInputText("Наименование провайдера сервис услуги:", PROVIDER)
                .save();
    }

    @Test(
            description = "1. Отправить Транзакцию №1 \"Перевод между счетами\", " +
                    "валюта расходного и приходного счета идентичны. (Сработает правило WR_01 Белое)",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -60);
        Transaction transaction = getTransferBetweenAccount();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Перевод на свой счет");
    }

    @Test(
            description = "2. Отправить Транзакцию №2 Оплата услуг от Клиента №1 на доверенного получателя",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 3);
        Transaction transaction = getTransferServicePayment();
        sendAndAssert(transaction);
        assertRuleResultForTheLastTransaction(RULE_NAME_WR_03, TRIGGERED, "В списке разрешенных найдены совпадающие параметры");
    }

    @Test(
            description = "3. Отправить Транзакцию №3 В  сторону государства" +
                    "(Сработает правило WR_02 Белое)",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getBudgetTransfer();
        sendAndAssert(transaction);
        assertRuleResultForTheLastTransaction(RULE_NAME_WR_02, TRIGGERED, "Транзакция в сторону государства");
    }

    @Test(
            description = "4. Отправить Транзакцию №4 Запрос на выдачу кредита с подозрительного IP" +
                    "(Белое правило WR_08_EntrustedAccMask не сработало)",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getGettingCredit();
        sendAndAssert(transaction);
        assertRuleResultForTheLastTransaction(RULE_NAME_WR_08, NOT_TRIGGERED, "Сработало персональное исключение 'ExR_05_GrayIP' белого правила");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME_WR_01;
    }

    private Transaction getTransferServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0)).withLoginHash(clientIds.get(0));
        transactionDataType
                .getServicePayment()
                .withProviderName(PROVIDER)
                .withServiceName(SERVICE)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00))
                .withSourceCardNumber(CARD)
                .getAdditionalField()
                .get(0)
                .withId("ACCOUNT")
                .withName("По номеру телефона")
                .withValue(TRUSTED_PHONE);
        return transaction;
    }

    private Transaction getTransferBetweenAccount() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0))
                .withLoginHash(clientIds.get(0));
        transactionDataType
                .getTransferBetweenAccounts()
                .withSourceCurrencyIsoCode(810)
                .withDestinationCurrencyIsoCode(810);
        return transaction;
    }

    private Transaction getBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0))
                .withLoginHash(clientIds.get(0));
        return transaction;
    }

    private Transaction getGettingCredit() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0))
                .withLoginHash(clientIds.get(0));
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIpAddress(GREY_IP);
        return transaction;
    }
}
