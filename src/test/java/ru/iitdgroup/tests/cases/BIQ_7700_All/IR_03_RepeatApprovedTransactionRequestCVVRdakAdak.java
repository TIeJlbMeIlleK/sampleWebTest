package ru.iitdgroup.tests.cases.BIQ_7700_All;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransactionRequestCVVRdakAdak extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String RULE_NAME_ALERT = "R01_GR_26_DocumentHashInGrayList";
    private static final String REFERENCE_TABLE_RDAK = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_TABLE_ALERT = "(Rule_tables) Подозрительные документы клиентов";
    private static final String REFERENCE_TABLE2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_TABLE3 = "(Policy_parameters) Параметры проведения ДАК";
    private final static String sourceCardNumber = "455634" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final static String firstNameAdak = "Григорий";
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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос CVC/CVV/CVP")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_TABLE_RDAK)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос CVC/CVV/CVP")
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
                time3.add(Calendar.YEAR, -48);

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withBirthDate(new XMLGregorianCalendarImpl(time3))
                        .withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time2))
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
                        .withSeries("46 28")
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

        String documentHash1;
        String documentHash2;

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
                    "Запрос CVC/CVV/CVP, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету",
            dependsOnMethods = "addClients"
    )

    public void transOuterCard() {
        time.add(Calendar.MINUTE, -20);
        Transaction transRequestCCV = getTransferRequestCCV();
        sendAndAssert(transRequestCCV);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос CVC/CVV/CVP», условия правила не выполнены");

        getIC().locateAlerts()
                .openFirst()
                .action("Подтвердить")
                .sleep(2);

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVTwo = getTransferRequestCCV();
        sendAndAssert(transRequestCCVTwo);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос CVC/CVV/CVP» условия правила не выполнены");

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
        Transaction transRequestCCVsourceCardNumber = getTransferRequestCCV();
        TransactionDataType transactionDataRequestCCVsourceCardNumber = transRequestCCVsourceCardNumber.getData().getTransactionData();
        transactionDataRequestCCVsourceCardNumber
                .getRequestCCV()
                .withSourceCardNumber("42753444" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12));
        sendAndAssert(transRequestCCVsourceCardNumber);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос CVC/CVV/CVP» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVTrigg = getTransferRequestCCV();
        sendAndAssert(transRequestCCVTrigg);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос CVC/CVV/CVP» транзакция с совпадающими реквизитами");
    }

    @Test(
            description = "Отправить транзакцию №1 от клиента №1 спустя 5 мин от транзакции №1, " +
                    "тип транзакции Запрос CVC/CVV/CVP, сумма 500, остаток на счету 10000р, реквизиты совпадают с транзакцией №1." +
                    "Перейти  в АЛЕРТ по транзакции №1:" +
                    "- Подтвердить правомочно по АДАК  - выполнив Action \"выполнить АДАК\", и ответив на АДАК верно." +
                    "После выполнения АДАК, статус АДАК = SUCCESS." +
                    "- выполнить Action  - \"Подтвердить\" для перехода Алерта и Транзакции в статус Обработано, резолюция Правомочно",
            dependsOnMethods = "transOuterCard"
    )

    public void transRequestCVVADAK() {
        Transaction transRequestCCV = getTransferRequestCCV();
        TransactionDataType transactionDataRequestCVV = transRequestCCV.getData().getTransactionData();
        transactionDataRequestCVV
                .getClientIds()
                .withDboId(clientIds.get(1));
        String transaction_id = transactionDataRequestCVV.getTransactionId();
        Long version = transactionDataRequestCVV.getVersion();
        sendAndAssert(transRequestCCV);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос CVC/CVV/CVP», условия правила не выполнены");

        getIC().locateAlerts().openFirst().action("Выполнить АДАК").sleep(1);
        assertTableField("Status:", "Ожидаю выполнения АДАК");

        Transaction adak = getAdak();
        TransactionDataType transactionADAK = adak.getData().getTransactionData();
        transactionADAK
                .withTransactionId(transaction_id)
                .withVersion(version);
        sendAndAssert(adak);

        getIC().locateAlerts().openFirst().action("Подтвердить").sleep(1);
        assertTableField("Resolution:", "Правомочно");
        assertTableField("Status:", "Обработано");
        assertTableField("Идентификатор клиента:", clientIds.get(1));
        assertTableField("Транзакция:", transaction_id);
        assertTableField("Статус АДАК:", "SUCCESS");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVsourceCardNumber = getTransferRequestCCV();
        TransactionDataType transactionDataRequestCCVsourceCardNumber = transRequestCCVsourceCardNumber.getData().getTransactionData();
        transactionDataRequestCCVsourceCardNumber
                .getRequestCCV()
                .withSourceCardNumber("42753444" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12));
        transactionDataRequestCCVsourceCardNumber
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transRequestCCVsourceCardNumber);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос CVC/CVV/CVP» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestCCVTrigg = getTransferRequestCCV();
        TransactionDataType transactionDataRequestCVVTrigg = transRequestCCVTrigg.getData().getTransactionData();
        transactionDataRequestCVVTrigg
                .getClientIds().withDboId(clientIds.get(1));
        sendAndAssert(transRequestCCVTrigg);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос CVC/CVV/CVP» транзакция с совпадающими реквизитами");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferRequestCCV() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getRequestCCV()
                .withSourceCardNumber(sourceCardNumber);
        return transaction;
    }

    private Transaction getAdak() {
        Transaction adak = getTransaction("testCases/Templates/ADAK.xml");
        adak.getData()
                .getServerInfo()
                .withPort(8050);
        TransactionDataType transactionADAK = adak.getData().getTransactionData()
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionADAK
                .getClientIds()
                .withDboId(clientIds.get(1))
                .withLoginHash(clientIds.get(1))
                .withCifId(clientIds.get(1))
                .withExpertSystemId(clientIds.get(1));
        transactionADAK.getAdditionalAnswer()
                .withAdditionalAuthAnswer(firstNameAdak);
        return adak;
    }
}
