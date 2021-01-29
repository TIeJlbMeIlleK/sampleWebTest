package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ADAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_ITEM3 = "(Policy_parameters) Параметры проведения ДАК";
    private static String TRANSACTION_ID;

    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static Random rand = new Random();

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time2;

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private Client client = null;

//TODO для прохождения теста в Alert должны быть внесены поля:Идентификатор клиента, Status (Алерта), Статус РДАК, status(транзакции)

//    @Test(
//            description = "Заполнить \"Вопросы для проведения ДАК\": codePhrase, birthDay, birthYear с установленными флагами \"Включено\" и \"Учавствует в РДАК\"" +
//                    "В справочник \"Параметры обработки событий\" внести транзакцию с клиентами по умолчанию, учавствуют РДАК и АДАК." +
//                    "в справочник Проверяемые Типы транзакции и Каналы ДБО внести тип транзакции" +
//                    "2. Заполнить \"Вопросы для проведения ДАК\": firstname с установленными флагами \"Включено\" и \"Учавствует в РДАК\". Ограничить количество символов 15"
//    )
//    public void enableRules() {
//
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .editRule(RULE_NAME)
//                .fillCheckBox("Active:", true)
//                .save()
//                .sleep(10);
//
//        getIC().locateTable(REFERENCE_ITEM)
//                .deleteAll()
//                .addRecord()
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
//                .select("Наименование канала:", "Мобильный банк")
//                .save();
//        getIC().locateTable(REFERENCE_ITEM1)
//                .deleteAll()
//                .addRecord()
//                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "По умолчанию")
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
//                .fillCheckBox("Требуется выполнение АДАК:", true)
//                .fillCheckBox("Требуется выполнение РДАК:", true)
//                .select("Наименование канала ДБО:", "Мобильный банк")
//                .save();
//        getIC().locateTable(REFERENCE_ITEM2)
//                .setTableFilter("Текст вопроса клиенту", "Equals", "Ваше имя")
//                .refreshTable()
//                .click(2)
//                .edit()
//                .fillCheckBox("Включено:", true)
//                .fillCheckBox("Участвует в АДАК:", true)
//                .fillCheckBox("Участвует в РДАК:", true).save()
//                .sleep(2);
//
//        getIC().locateTable(REFERENCE_ITEM3)
//                .findRowsBy()
//                .match("Код значения", "AUTHORISATION_QUESTION_CODE")
//                .click()
//                .edit()
//                .fillInputText("Значение:", "20000")
//                .save();
//    }

    @Test(
            description = "В САФ завести клиента № 1"
            //  dependsOnMethods = "enableRules"
    )

    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "Провести транзакцию №1 не отвечать на АДАК более 20 сек",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(30);//не отвечать на АДАК больше 20сек

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "TIMEOUT");

    }

    @Test(
            description = "Провести транзакцию № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress("178.219.186.12");
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);
    }

    @Test(
            description = "отказаться от АДАК по транзакции №2",
            dependsOnMethods = "transaction2"
    )
    public void adak2() {
        Transaction adak = getAdak();
        TransactionDataType transactionData = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getAdditionalAnswer().withAdditionalAuthCancel(true);//отказаться от АДАК
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withTransactionId(TRANSACTION_ID);
        transactionData.getAdditionalAnswer()
                .withAdditionalAuthAnswer("Ольга");
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();

        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "REFUSE");
    }

    @Test(
            description = "Провести транзакцию № 3",
            dependsOnMethods = "adak2"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress("178.219.186.12");
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);
    }

    @Test(
            description = "ответить на АДАК неверно по транзакции №3",
            dependsOnMethods = "transaction3"
    )
    public void adak3() {
        Transaction adak = getAdak();
        TransactionDataType transactionData = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withTransactionId(TRANSACTION_ID);
        transactionData.getAdditionalAnswer()
                .withAdditionalAuthAnswer("Юлия");
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "WRONG");
    }

    @Test(
            description = "Провести транзакцию № 4",
            dependsOnMethods = "adak3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress("178.219.186.12");
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);
    }

    @Test(
            description = "Выполнить АДАК ответить верно по транзакции №4",
            dependsOnMethods = "transaction4"
    )
    public void adak4() {
        Transaction adak = getAdak();
        TransactionDataType transactionData = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withTransactionId(TRANSACTION_ID);
        transactionData.getAdditionalAnswer()
                .withAdditionalAuthAnswer("Ольга");
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "SUCCESS");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getAdak() {
        Transaction adak = getTransaction("testCases/Templates/ADAK.xml");
        adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return adak;
    }
}