package ru.iitdgroup.tests.cases.BIQ_7700_All;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.dbdriver.With;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import java.sql.*;

public class IR_03_RepeatApprovedTransactionOuterCardRdakAdak extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String RULE_NAME_ALERT = "R01_GR_26_DocumentHashInGrayList";
    private static final String REFERENCE_TABLE_RDAK = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_TABLE_ALERT = "(Rule_tables) Подозрительные документы клиентов";
    private static final String REFERENCE_TABLE2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_TABLE3 = "(Policy_parameters) Параметры проведения ДАК";

    private String documentHash1;
    private String documentHash2;
    private final static String firstNameAdak = "Спиридон";
    private String transaction_id;
    private Long version;

    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final GregorianCalendar time3 = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Илья", "Кузнецов", "Андреевич"}, {firstNameAdak, "Павлов", "Олегович"}};

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_ALERT)
                .activate();
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .edit()
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", true)
                .fillCheckBox("РДАК выполнен:", true)
                .fillCheckBox("Требовать совпадения остатка на счете:", true)
                .fillInputText("Длина серии:", "2")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachIR03SelectAllType()
                .sleep(15);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод с платежной карты стороннего банка на платежную карту РСХБ")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE_RDAK)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод с платежной карты стороннего банка на платежную карту РСХБ")
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Учитывать маску правила:", false)
                .select("Наименование канала ДБО:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE3)
                .findRowsBy()
                .match("Код значения", "AUTHORISATION_QUESTION_CODE")
                .click()
                .edit()
                .fillInputText("Значение:", "200000")
                .save();
        getIC().locateTable(REFERENCE_TABLE2)
                .findRowsBy()
                .match("Текст вопроса клиенту", "Ваше имя")
                .click()
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true)
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 8);
                String numberPassword = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
                String organization = "МВД "+ new RandomString(10).nextString();
                Client client = new Client("testCases/Templates/client.xml");
                time2.add(Calendar.YEAR, -12);
                time2.add(Calendar.YEAR, -48);

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withBirthDate(new XMLGregorianCalendarImpl(time3))
                        .withPasswordRecoveryDateTime(time2)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                client.getData()// для получения уникального Hash документа, нужно внести изменения у клиента в этом блоке
                        .getClientData()
                        .getClientDocument()
                        .get(0)
                        .withDocType("21")
                        .withSeries("46 25")
                        .withNumber(numberPassword)
                        .withIssueDate(new XMLGregorianCalendarImpl(time2))
                        .withOrganization(organization);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            String[][] hash = getDatabase()//сохраняем в переменную Hash действующего документа из карточки клиента
                    .select()
                    .field("ACTIVE_DOCUMENT_HASH")
                    .from("Client")
                    .sort("id", false)
                    .limit(2)
                    .get();
            documentHash1 = hash[0][0];
            System.out.println(documentHash1);
            documentHash2 = hash[1][0];
            System.out.println(documentHash2);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        getIC().locateTable(REFERENCE_TABLE_ALERT)
                .deleteAll()
                .addRecord()
                .fillInputText("Hash документа:", documentHash1)
                .select("Причина занесения:", "Внешняя система")
                .save();
        getIC().locateTable(REFERENCE_TABLE_ALERT)
                .addRecord()
                .fillInputText("Hash документа:", documentHash2)
                .select("Причина занесения:", "Внешняя система")
                .save();
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод с платежной карты стороннего банка на платежную карту РСХБ, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету",
            dependsOnMethods = "addClients"
    )

    public void transOuterCard() {
        time.add(Calendar.MINUTE, -20);
        Transaction transOuterCard = getOuterCardTransfer();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить")
                .sleep(2);

        time.add(Calendar.SECOND, 20);
        Transaction transOuterOutside = getOuterCardTransfer();
        TransactionDataType transactionDataOuterOutside = transOuterOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .sleep(1)
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .MyPayment()
                .action("Подтвердить")
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "Обработано");
        assertTableField("Статус РДАК:", "SUCCESS");
        assertTableField("status:", "Обработано");
        assertTableField("Resolution:", "Правомочно");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBalance = getOuterCardTransfer();
        TransactionDataType transactionDataOuterAccountBalance = transOuterAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transOuterAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterDeviation = getOuterCardTransfer();
        TransactionDataType transactionDataOuterDeviation = transOuterDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterDeviation
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод с платежной карты стороннего банка на платежную карту РСХБ» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Перевод с платежной карты стороннего банка на платежную карту РСХБ, сумма 500, остаток на счету 10000р, реквизиты совпадают с транзакцией №1." +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- Подтвердить правомочно по АДАК  - выполнив Action \"выполнить АДАК\", и ответив на АДАК верно." +
                    "После выполнения АДАК, статус АДАК = SUCCESS." +
                    "- выполнить Action  - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "transOuterCard"
    )

    public void transOuterADAK() {

        Transaction transOuterCard = getOuterCardTransfer();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterCard
                .getClientIds()
                .withDboId(clientIds.get(1));
        transaction_id = transactionDataOuterCard.getTransactionId();
        version = transactionDataOuterCard.getVersion();
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Выполнить АДАК").sleep(1);
        assertTableField("Status:", "Ожидаю выполнения АДАК");

        Transaction adak = getAdak();
        TransactionDataType transactionADAK = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionADAK
                .getClientIds()
                .withDboId(clientIds.get(1))
                .withLoginHash(clientIds.get(1))
                .withCifId(clientIds.get(1))
                .withExpertSystemId(clientIds.get(1));
        transactionADAK
                .withTransactionId(transaction_id)
                .withVersion(version);
        transactionADAK.getAdditionalAnswer()
                .withAdditionalAuthAnswer(firstNameAdak);
        sendAndAssert(adak);

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Status:", "Обработано");
        assertTableField("Идентификатор клиента:", clientIds.get(1));
        assertTableField("Транзакция:", transaction_id);
        assertTableField("Статус АДАК:", "SUCCESS");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBalance = getOuterCardTransfer();
        TransactionDataType transactionDataOuterAccountBalance = transOuterAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        transactionDataOuterAccountBalance
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transOuterAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterDeviation = getOuterCardTransfer();
        TransactionDataType transactionDataOuterDeviation = transOuterDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterDeviation
                .getClientIds().withDboId(clientIds.get(1));
        transactionDataOuterDeviation
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод с платежной карты стороннего банка на платежную карту РСХБ» транзакция с совпадающими реквизитами");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getOuterCardTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_CARD_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500.00));
        return transaction;
    }

    private Transaction getAdak() {
        Transaction transaction = getTransaction("testCases/Templates/ADAK.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
