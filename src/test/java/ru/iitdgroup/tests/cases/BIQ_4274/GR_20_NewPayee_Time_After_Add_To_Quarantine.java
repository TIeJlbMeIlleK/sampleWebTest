package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_Time_After_Add_To_Quarantine extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String TABLE_GOOD = "(Rule_tables) Доверенные получатели";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String CARDNUMBER = "43787277" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    private static String transactionID;
    private final static String PHONE_TRUSTED = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final static String PAYEE_NAME_MT_SYSTEM = "Парамонов Виктор Васильевич";
    private final static String PROVIDER = "Beeline";
    private final static String SERVICE = "Mobile";
    private final static String MT_SYSTEM_NAME = "QIWI";

    //        TODO требуется реализовать настройку блока Alert Scoring Model по правилу + Alert Scoring Model общие настройки

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило GR_20 срабатывает при наличии записи в \"Доверенные получатели\""+ " тк№3 -- BIQ4274");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
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
    }

    @Test(
            description = "Добавление в карантин получателей по клиенту",
            dependsOnMethods = "step0"
    )
    public void addToGoodPayee() {
        getIC().locateTable(TABLE_QUARANTINE)
                .deleteAll();
        getIC().locateTable(TABLE_GOOD)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", PHONE_TRUSTED)
                .fillInputText("Наименование сервиса:", SERVICE)
                .fillInputText("Наименование провайдера сервис услуги:", PROVIDER)
                .fillInputText("Номер карты получателя:",CARDNUMBER)
                .fillInputText("Наименование системы денежных переводов:", MT_SYSTEM_NAME)
                .fillInputText("Наименование получателя в системе денежных переводов:", PAYEE_NAME_MT_SYSTEM)
                .fillInputText("Страна получателя в системе денежных переводов:","РОССИЯ")
                .save();
    }

    @Test(
            description = "1. Провести транзакции:" +
                    "1.1. Оплата услуг",
            dependsOnMethods = "addToGoodPayee"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -20);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time.getTime());

        Transaction transaction = getTransactionSERVICE_PAYMENT();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getServicePayment()
                .withProviderName(PROVIDER)
                .withServiceName(SERVICE)
                .getAdditionalField()
                .get(0)
                .withId("ACCOUNT")
                .withName("Номер телефона")
                .withValue(PHONE_TRUSTED);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);

        getIC().locateTable(TABLE_GOOD)
                .findRowsBy()
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", PHONE_TRUSTED)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
    }

    @Test(
            description = "Провести транзакции" +
                    "1.2. Перевод на карту",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 5);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time.getTime());

        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withDestinationCardNumber(CARDNUMBER);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);

        getIC().locateTable(TABLE_GOOD)
                .findRowsBy()
                .match("Номер карты получателя", CARDNUMBER)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
    }

    @Test(
            description = "Провести транзакции" +
                    "1.3. Перевод через систему денежных переводов",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 5);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time.getTime());

        Transaction transaction = getTransactionSDP();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getMTSystemTransfer()
                .withReceiverCountry("РОССИЯ")
                .withReceiverName(PAYEE_NAME_MT_SYSTEM)
                .withMtSystem("QIWI");
        transactionID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);

        getIC().locateTable(TABLE_GOOD)
                .findRowsBy()
                .match("Наименование получателя в системе денежных переводов", PAYEE_NAME_MT_SYSTEM)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
    }

    @Test(
            description = "Провести транзакции" +
                    "1.4. Изменение перевода через систему денежных переводов (изменение перевода 1.4) с изменением получателя",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 5);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time.getTime());

        Transaction transaction = getTransactionSdpRefactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getMTTransferEdit()
                .withEditingTransactionId(transactionID)
                .getSystemTransferCont()
                .withMtSystem("QIWI")
                .withReceiverName(PAYEE_NAME_MT_SYSTEM)
                .withReceiverCountry("РОССИЯ")
                .withAmountInSourceCurrency(BigDecimal.valueOf(15000.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);

        getIC().locateTable(TABLE_GOOD)
                .findRowsBy()
                .match("Наименование получателя в системе денежных переводов", PAYEE_NAME_MT_SYSTEM)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
    }

    @Test(
            description = "Провести транзакции" +
                    "1.5. Перевод по номеру телефона",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE, 5);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time.getTime());

        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone(PHONE_TRUSTED);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);

        getIC().locateTable(TABLE_GOOD)
                .findRowsBy()
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", PHONE_TRUSTED)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
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
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/MT_SYSTEM_TRANSFER_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSdpRefactor() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
