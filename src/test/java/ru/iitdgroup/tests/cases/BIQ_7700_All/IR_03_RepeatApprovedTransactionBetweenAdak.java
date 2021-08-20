package ru.iitdgroup.tests.cases.BIQ_7700_All;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_03_RepeatApprovedTransactionBetweenAdak extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String RULE_NAME_ALERT = "R01_ExR_05_GrayIP";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_TABLE_ALERT = "(Rule_tables) Подозрительные IP адреса";
    private static final String REFERENCE_TABLE_ADAK = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_TABLE2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_TABLE3 = "(Policy_parameters) Параметры проведения ДАК";
    private final String ipAddress = "95.73.149.81";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ирина", "Зыкова", "Игоревна"}};

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
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод между счетами")
                .select("Наименование канала:", "Мобильный банк")
                .save();

        getIC().locateTable(REFERENCE_TABLE_ADAK)
                .deleteAll()
                .addRecord()
                .getElement("Наименование группы клиентов:")
                .refresh()
                .getSelect("Группа по умолчанию")
                .tapToSelect()
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .fillCheckBox("Учитывать маску правила:", false)
                .select("Наименование канала ДБО:", "Мобильный банк")
                .getElement("Тип транзакции:")
                .getSelect("Перевод между счетами")
                .tapToSelect()
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
            description = "Отправить транзакцию №1 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Перевод между счетами, сумма 500, остаток на счету 10000р, реквизиты совпадают с транзакцией №1." +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- Подтвердить правомочно по АДАК  - выполнив Action \"выполнить АДАК\", и ответив на АДАК верно." +
                    "После выполнения АДАК, статус АДАК = SUCCESS." +
                    "- выполнить Action  - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "addClients"
    )

    public void transactiotransBetweenADAK() {
        Transaction transaction = getTransactionBetween();
        TransactionDataType transactionDataType =transaction.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00));
        sendAndAssert(transaction);
        String transactionId = transactionDataType.getTransactionId();
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод между счетами», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Выполнить АДАК").sleep(1);
        assertTableField("Status:", "Ожидаю выполнения АДАК");

        Transaction adak = getAdak();
        TransactionDataType transactionADAK = adak.getData().getTransactionData();
        transactionADAK
                .withTransactionId(transactionId);
        sendAndAssert(adak);

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Status:", "Обработано");
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Транзакция:", transactionId);
        assertTableField("Статус АДАК:", "SUCCESS");
    }

    @Test(
            description = "Отправить транзакцию №3 от клиента №1 спустя 5 мин от транзакции №1," +
                    "тип транзакции Перевод между счетами, сумма 500р, остаток на счету 8000р, реквизиты совпадают с транзакцией №1 ",
            dependsOnMethods = "transactiotransBetweenADAK"
    )

    public void transactiotransBetweenADAKTrigg() {
        Transaction transaction = getTransactionBetween();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод между счетами» транзакция с совпадающими реквизитами");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionBetween() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        transactionData
                .getClientDevice().getAndroid().withIpAddress(ipAddress);
        return transaction;
    }

    private Transaction getAdak() {
        Transaction adak = getTransaction("testCases/Templates/ADAK.xml");
        TransactionDataType transactionADAK = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionADAK
                .getClientIds()
                .withDboId(clientIds.get(0))
                .withLoginHash(clientIds.get(0))
                .withCifId(clientIds.get(0))
                .withExpertSystemId(clientIds.get(0));
        transactionADAK.getAdditionalAnswer()
                .withAdditionalAuthAnswer("Ирина");
        return adak;
    }
}
