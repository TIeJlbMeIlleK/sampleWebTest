package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
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

public class R01_GR_30_SeriesRequestCardIssue extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_30_SeriesRequestCardIssue";
    private static final String RULE_NAME_ALERT = "R01_ExR_06_GrayDevice";

    private final GregorianCalendar time = new GregorianCalendar();

    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_GREY_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String GREY_IFV = new RandomString(25).nextString();

    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void enableRule() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_ALERT)
                .activate()
                .sleep(2)
                .editRule(RULE_NAME)
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Длина серии:", "2")
                .fillCheckBox("Active:", true)
                .save()
                .detachDelGroup()
                .attachTransactionNew("Заявка на выпуск карты")
                .sleep(10);

        getIC().locateTable(REFERENCE_GREY_IFV)
                .deleteAll()
                .addRecord()
                .fillInputText("Identifier for vendor:", GREY_IFV)
                .save();
    }

    @Test(
            description = "Настройка справочников для РДАК",
            dependsOnMethods = "enableRule"
    )

    public void rdakSet() {
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Заявка на выпуск карты")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
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
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "VES_TIMEOUT")
                .click()
                .edit()
                .fillInputText("Значение:", "300")
                .save();

        //TODO WF Алерт "Взять в работу для выполнения РДАК" уже настроен
//        getIC().locateWorkflows()
//                .openRecord("Alert Workflow")
//                .openAction("Взять в работу для выполнения РДАК")
//                .clearAllStates()
//                .addFromState("Any State")
//                .addToState("На выполнении РДАК")
//                .save();
//TODO Если не внесены статусы РДАК в таблицу Перечень статусов для которых применяется РДАК, то запустить строки ниже
        //getDatabase().deleteWhere("LIST_APPLY_RDAKSTATUS", "");
        //getDatabase().insertRows("LIST_APPLY_RDAKSTATUS", new String[]{"'rdak_underfire', 'RDAK_Done'", "'Wait_RDAK', 'RDAK_Done'"});
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "rdakSet"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
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
            description = "Отправить транзакцию №1 \"Перевод по номеру телефона\"",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Отправить транзакцию №2  \"Заявка на выпуск карты\" ",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Отправить транзакцию №3  \"Заявка на выпуск карты\", спустя минуту после транзакции №2",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Отправить транзакцию №4  \"Заявка на выпуск карты\", спустя 1 минутe после транзакции №3",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_TRIGGERED);
    }

    @Test(
            description = "Отправить транзакцию №5  \"Заявка на выпуск карты\", спустя 11 минут после транзакции №4",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "Отправить транзакцию №6  \"Заявка на выпуск карты\", спустя минуту после транзакции №5" +
                    "-- Перейти в Алерт по транзакции №6 и Подтвердить правомочно по РДАК",
            dependsOnMethods = "step5"
    )
    public void step6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .MyPayment()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "SUCCESS");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Отправить транзакцию №7 \"Заявка на выпуск карты\", спустя минуту после транзакции №6",
            dependsOnMethods = "step6"
    )
    public void step7() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
        getIC().close();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_IOC.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(GREY_IFV);
        return transaction;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

}
