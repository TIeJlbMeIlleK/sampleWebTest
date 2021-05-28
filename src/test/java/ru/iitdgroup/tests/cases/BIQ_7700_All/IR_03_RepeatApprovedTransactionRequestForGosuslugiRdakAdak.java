package ru.iitdgroup.tests.cases.BIQ_7700_All;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransactionRequestForGosuslugiRdakAdak extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String RULE_NAME_ALERT = "R01_ExR_05_GrayIP";
    private static final String REFERENCE_TABLE_RDAK = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_TABLE_ALERT = "(Rule_tables) Подозрительные IP адреса";
    private static final String REFERENCE_TABLE2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_TABLE3 = "(Policy_parameters) Параметры проведения ДАК";

    private static final int gosuslugiRequestType = 15;
    private static final String firstNameAdak = "Николай";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Петр", "Урин", "Семенович"}, {firstNameAdak, "Румянцева", "Григорьевна"}};
    private final String ipAddress = "95.73.149.81";

    private String transaction_id;
    private Long version;

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос в госуслуги")
                .select("Наименование канала:", "Мобильный банк")
                .save();

        getIC().locateTable(REFERENCE_TABLE_RDAK)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос в госуслуги")
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

        getIC().locateTable(REFERENCE_TABLE_ALERT)
                .deleteAll()
                .addRecord()
                .fillInputText("IP устройства:", ipAddress)
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
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
    }

    @Test(
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Запрос в госуслуги, проверить на RDAK, отклонение суммы," +
                    "на совпадение остатка по счету, на длину серии, productName, sourceProduct",
            dependsOnMethods = "addClients"
    )

    public void transOpenDeposit() {
        time.add(Calendar.MINUTE, -20);
        Transaction transRequestForGosuslugi = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugi = transRequestForGosuslugi.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить")
                .sleep(2);

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiTwo = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiTwo = transRequestForGosuslugiTwo.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiTwo);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .sleep(2)
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
        Transaction transRequestForGosuslugiType = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiType = transRequestForGosuslugiType.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataRequestForGosuslugiType
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(10);
        sendAndAssert(transRequestForGosuslugiType);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiTrigg = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiTrigg = transRequestForGosuslugiTrigg.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiTrigg);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос в госуслуги» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Запрос в госуслуги, сумма 500, остаток на счету 10000р, реквизиты совпадают с транзакцией №1." +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- Подтвердить правомочно по АДАК  - выполнив Action \"выполнить АДАК\", и ответив на АДАК верно." +
                    "После выполнения АДАК, статус АДАК = SUCCESS." +
                    "- выполнить Action  - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "transOpenDeposit"
    )

    public void transBetweenADAK() {
        Transaction transRequestForGosuslugi = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugi = transRequestForGosuslugi.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataRequestForGosuslugi
                .getClientIds().withDboId(clientIds.get(1));
        transaction_id = transactionDataRequestForGosuslugi.getTransactionId();
        version = transactionDataRequestForGosuslugi.getVersion();
        sendAndAssert(transRequestForGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");

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
        Transaction transRequestForGosuslugiType = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiType = transRequestForGosuslugiType.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataRequestForGosuslugiType
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(10);
        transactionDataRequestForGosuslugiType
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transRequestForGosuslugiType);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiTrigg = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiTrigg = transRequestForGosuslugiTrigg.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiTrigg);
        transactionDataRequestForGosuslugiTrigg
                .getClientIds()
                .withDboId(clientIds.get(1));
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос в госуслуги» транзакция с совпадающими реквизитами");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferRequestForGosuslugi() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(gosuslugiRequestType);
        transaction.getData().getTransactionData()
                .getClientDevice()
                .getAndroid()
                .withIpAddress(ipAddress);
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
