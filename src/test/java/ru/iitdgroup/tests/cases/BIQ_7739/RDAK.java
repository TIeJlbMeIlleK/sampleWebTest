package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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

public class RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String RDAK = "(Policy_parameters) Перечень статусов для которых применять РДАК";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String IP_ADRESS = "178.219.186.12";
    private final String[][] names = {{"Наталья", "Сколкина", "Олеговна"}};

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

//TODO для прохождения теста в Alert должны быть внесены поля:Идентификатор клиента, Status (Алерта), Статус РДАК, status(транзакции)

    @Test(
            description = "Занести транзакция в проверяемые, " +
                    "в справочнике событий выбрать клиентов по умолчанию и применить РДАК. " +
                    "Выбрать правило GR_20." +
                    "Заполнить \"Вопросы для проведения ДАК\": " +
                    "registrationHouse, registrationStreet, birthDate с установленными флагами \"Включено\" и \"Учавствует в РДАК\""
    )
    public void enableRules() {

        getIC().locateRules()
                .selectVisible()
                .deactivate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Заявка на выпуск карты")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Заявка на выпуск карты")
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
                .fillCheckBox("Участвует в РДАК:", true)
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .setTableFilter("Текст вопроса клиенту", "Equals", "Дата вашего рождения полностью")
                .refreshTable()
                .click(2)
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true)
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
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
    }

    @Test(
            description = "Провести транзакции № 1",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .notMyPayment()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "WRONG");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .clientNotConfirmed()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "NOT_CONFIRMED_CLIENT");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 3",
            dependsOnMethods = "transaction1"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .theClientWillCallHimself()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "На выполнении РДАК");
        assertTableField("Статус РДАК:", "CLIENT_CALL");
        assertTableField("status:", "Подозрительная");

        getIC().locateAlerts()
                .openFirst()
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .NotKnown()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "UNKNOWN");
        assertTableField("status:", "Подозрительная");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIpAddress(IP_ADRESS);
        return transaction;
    }
}
