package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
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
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String namesAdak = "Ольга";
    private final String[][] names = {{namesAdak, "Петушкова", "Ильинична"}};
    private final String ipAdress = "178.219.186.12";

//TODO для прохождения теста в Alert должны быть внесены поля:Идентификатор клиента, Status (Алерта), Статус РДАК, status(транзакции)
    //TODO должен быть включен только один вопрос в справочнике Вопросы ДАК : Ваше имя. Остальные вопросы не активны.(выключены)

    @Test(
            description = "Заполнить \"Вопросы для проведения ДАК\": codePhrase, birthDay, birthYear с установленными флагами \"Включено\" и \"Учавствует в РДАК\"" +
                    "В справочник \"Параметры обработки событий\" внести транзакцию с клиентами по умолчанию, учавствуют РДАК и АДАК." +
                    "в справочник Проверяемые Типы транзакции и Каналы ДБО внести тип транзакции" +
                    "2. Заполнить \"Вопросы для проведения ДАК\": firstname с установленными флагами \"Включено\" и \"Учавствует в РДАК\". Ограничить количество символов 15"
    )
    public void enableRules() {

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .select("Наименование канала ДБО:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .setTableFilter("Текст вопроса клиенту", "Equals", "Ваше имя")
                .refreshTable()
                .click(2)
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true).save()
                .sleep(2);

        getIC().locateTable(REFERENCE_ITEM3)
                .findRowsBy()
                .match("Код значения", "AUTHORISATION_QUESTION_CODE")
                .click()
                .edit()
                .fillInputText("Значение:", "10000")
                .save();
        commandServiceMock.run();
    }

    @Test(
            description = "В САФ завести клиента № 1",
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
    }

    @Test(
            description = "Провести транзакцию №1 не отвечать на АДАК более 20 сек",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(25);//не отвечать на АДАК больше 20сек

        getIC().locateAlerts()
                .openFirst().sleep(2);
        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:", "TIMEOUT");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
    }

    @Test(
            description = "Провести транзакцию № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = adak.getData().getTransactionData();
        transactionData
                .getAdditionalAnswer()
                .withAdditionalAuthCancel(true);//отказаться от АДАК
        transactionData
                .withTransactionId(TRANSACTION_ID);
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = adak.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = adak.getData().getTransactionData();
        transactionData
                .withTransactionId(TRANSACTION_ID);
        transactionData.getAdditionalAnswer()
                .withAdditionalAuthAnswer(namesAdak);
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "SUCCESS");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "adak4"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100.00));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(ipAdress);
        return transaction;
    }

    private Transaction getAdak() {
        Transaction adak = getTransaction("testCases/Templates/ADAK.xml");
        adak.getData().getServerInfo().withPort(8050);
        TransactionDataType adakDate = adak.getData().getTransactionData()
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        adakDate
                .getClientIds()
                .withDboId(clientIds.get(0));
        adakDate
                .getAdditionalAnswer()
                .withAdditionalAuthAnswer(namesAdak);
        return adak;
    }
}