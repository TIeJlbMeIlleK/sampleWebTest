package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_Quarantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_TRUSTED = "(Rule_tables) Доверенные получатели";
    private static final String LOCAL_TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";
    private final String[][] names =
            {{"Вероника", "Жукова", "Игоревна"},
                    {"Петр", "Смирнов", "Вячеславович"},
                    {"Семен", "Мотиков", "Петрович"},
                    {"Зинаида", "Логинова", "Павловна"},
                    {"Людмила", "Калинина", "Михайловна"},
                    {"Валентина", "Семченкова", "Ивановна"}};
    private final String payeePhone = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String payeePhone2 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String destantionCardNumber = "42789652" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destantionCardNumber2 = "42789652" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destantionCardNumber3 = "42789652" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destantionCardNumber4 = "42789652" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String cardNumberTrusted = "42789652" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount = "4278" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    //        TODO требуется реализовать настройку блока Alert Scoring Model по правилу + Alert Scoring Model общие настройки

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);

        getIC().locateTable(LOCAL_TABLE)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Создаем клиента" +
                    "Занести произвольный номер карты получателя  в 'Доверенные получатели' для клиента №5" +
                    "Занести произвольный номер карты получателя  в 'Карантин получателей' для клиента №4",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        time1.add(Calendar.HOUR, -50);
        try {
            for (int i = 0; i < 6; i++) {
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

        getIC().locateTable(TABLE_TRUSTED)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(4))
                .fillInputText("Номер карты получателя:", cardNumberTrusted)
                .save();
        getIC().locateTable(TABLE_QUARANTINE)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер Карты получателя:", destantionCardNumber2)
                .fillInputText("Дата занесения:", format.format(time1.getTime()))//дата занесения 2 дня назад
                .fillInputText("Дата последней авторизованной транзакции:", format.format(time1.getTime()))//дата занесения 2 дня назад
                .fillUser("ФИО Клиента:", clientIds.get(3))
                .save();
    }

    @Test(
            description = "1. Провести транзакцию № 1 на уникальный номер телефона(без указания 'Номера карты' и 'БИКСЧЕТ')," +
                    "для клиента № 1 'Перевод по номеру телефона'",
            dependsOnMethods = "addClient"
    )

    public void phoneNumberTransferPayeePhone() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(payeePhone)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель добавлен в список карантина");
    }

    @Test(
            description = "2. Провести транзакцию № 2 на уникальный номер карты (без указания 'Номера телефона'" +
                    "и 'БИКСЧЕТ'), для клиента № 2 'Перевод по номеру телефона'",
            dependsOnMethods = "phoneNumberTransferPayeePhone"
    )

    public void phoneNumberTransferDestinationCard() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(null)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(destantionCardNumber);
        transactionData
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель добавлен в список карантина");
    }

    @Test(
            description = "3. Провести транзакцию № 3 уникальный БИКСЧЕТ(без указания 'Номера карты'" +
                    "и 'Номера телефона'), для клиента № 3 'Перевод по номеру телефона'",
            dependsOnMethods = "phoneNumberTransferDestinationCard"
    )

    public void phoneNumberTransferBIKAccount() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(null)
                .withPayeeName(null)
                .withBIK(BIK)
                .withPayeeAccount(payeeAccount)
                .withDestinationCardNumber(null);
        transactionData
                .getClientIds().withDboId(clientIds.get(2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель добавлен в список карантина");
    }

    @Test(
            description = "4. Провести транзакцию №4 'Перевод по номеру телефона' от клиента №2, спустя 2 мин " +
                    "в транзакции указан только номер карты",
            dependsOnMethods = "phoneNumberTransferBIKAccount"
    )

    public void phoneNumberTransferDestinationCard2() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(null)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(destantionCardNumber);
        transactionData
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель недавно находится в карантине");
    }

    @Test(
            description = "7. Провести транзакцию №6 'Перевод по номеру телефона' от клиента №4, " +
                    "номер карты из Карантина, дата занесения 2 дня назад",
            dependsOnMethods = "phoneNumberTransferDestinationCard2"
    )

    public void phoneNumberTransferDestinationCardQuarantine() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(null)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(destantionCardNumber2);
        transactionData
                .getClientIds().withDboId(clientIds.get(3));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Получатель уже находится в карантине");
    }

    @Test(
            description = "9. Провести транзакцию № 7 'Перевод по номеру телефона', от клиента №5, на карту получателя в доверенных ",
            dependsOnMethods = "phoneNumberTransferDestinationCardQuarantine"
    )

    public void phoneNumberTransferTrusted() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(null)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(cardNumberTrusted);
        transactionData
                .getClientIds().withDboId(clientIds.get(4));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Получатель найден в списке разрешенных");
    }

    @Test(
            description = "10. Провести транзакцию №8 'Перевод на карту', от клиента №5, на карту получателя в доверенных",
            dependsOnMethods = "phoneNumberTransferTrusted"
    )

    public void cardTransferTrusted() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber(cardNumberTrusted);
        transactionData
                .getClientIds().withDboId(clientIds.get(4));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Получатель найден в списке разрешенных");
    }

    @Test(
            description = "11. Провести транзакцию №9 'Перевод по номеру телефона' от клиента №6 с уникальным номером телефона и номером карты",
            dependsOnMethods = "cardTransferTrusted"
    )

    public void phoneNumberTransferPayeePhoneCard() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(payeePhone2)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(destantionCardNumber3);
        transactionData
                .getClientIds().withDboId(clientIds.get(5));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель добавлен в список карантина");
    }

    @Test(
            description = "12. Провести транзакцию №10 'Перевод по номеру телефона' от клиента №6 " +
                    "с телефоном из транзакции № 9, но новым номером карты",
            dependsOnMethods = "phoneNumberTransferPayeePhoneCard"
    )

    public void phoneNumberTransferPayeePhoneCardNew() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(payeePhone2)
                .withPayeeName(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withDestinationCardNumber(destantionCardNumber4);
        transactionData
                .getClientIds().withDboId(clientIds.get(5));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель недавно находится в карантине");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
