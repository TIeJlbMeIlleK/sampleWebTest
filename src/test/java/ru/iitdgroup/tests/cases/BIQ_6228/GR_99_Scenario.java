package ru.iitdgroup.tests.cases.BIQ_6228;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_99_Scenario extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_99_Scenario";
    private static final String TABLE_NAME = "(Policy_parameters) Блоки сценариев";
    private final GregorianCalendar time_1 = new GregorianCalendar();
    private final GregorianCalendar time_2 = new GregorianCalendar();
    private final GregorianCalendar time_3 = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Леонид", "Жуков", "Игоревич"}, {"Ксения", "Новикова", "Сергеевна"}, {"Илья", "Птичкин", "Олегович"}};

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void enableRules() {
        String ID_BLOK1;
        String ID_BLOK2;

        Table table = getIC().locateTable(TABLE_NAME);
        int count = table
                .findRowsBy()
                .match("Transaction Type", "Перевод на счет другому лицу")
                .countMatchedRows();

        if (count == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Перевод на счет другому лицу")
                    .click()
                    .edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                    .save();
        }

        ID_BLOK1 = copyThisLine("ID:");

        int count1 = table
                .findRowsBy()
                .match("Transaction Type", "Перевод на карту другому лицу")
                .countMatchedRows();

        if (count1 == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Перевод на карту другому лицу")
                    .click()
                    .edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
                    .save();
        }

        ID_BLOK2 = copyThisLine("ID:");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Максимальный промежуток времени (смена IMSI):", "20")
                .fillInputText("Максимальный промежуток времени (подключение ДБО ФЛ):", "")
                .fillInputText("Максимальный промежуток времени (Смена данных клиента):", "")
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:", "20")
                .fillInputText("Логическое выражение:", ID_BLOK1 + "&&" + ID_BLOK2)
                .save()
                .sleep(5);

        getIC().locateWorkflows()
                .openRecord("Клиент Workflow")
                .openAction("Изменен IMSI аутент_true")
                .clearFieldMappings()
                .addFieldMapping("Изменен IMSI телефона для аутентификации", "true", null)
                .addFieldMapping("Дата изменения IMSI телефона для аутентификации", "DateAdd(SECOND,-2,Now())", null)
                .save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        time_3.add(Calendar.MINUTE, -60);
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                if (i == 0 || i == 1) {
                    client.getData()
                            .getClientData().getClient().withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time_1));
                }else {
                    client.getData()
                            .getClientData().getClient().withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time_3));
                }

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
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsClient("Изменен IMSI аутент_true");
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(1))
                .runReport()
                .openFirst()
                .getActionsClient("Изменен IMSI аутент_true");
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(2))
                .runReport()
                .openFirst()
                .getActionsClient("Изменен IMSI аутент_true");
    }

    @Test(
            description = "Клиент №1" +
                    "1. Провести транзакцию №1 Перевод на счет другому лицу, сумма 1001" +
                    "2. Провести транзакцию №2 Перевод на карту, сумма 1001" +
                    "3. Провести транзакцию № 3 Перевод с платежной карты, DocumentSaveTimeStamp больше на 5 минут с момента транзакции № 2, сумма 1001",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionOuterTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, SCENARIO_BLOCK_TRUE);

        time_1.add(Calendar.MINUTE, 12);
        Transaction transaction3 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData3 = transaction3.getData().getTransactionData();
        transactionData3
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction3);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Клиент №2" +
                    "1. Провести транзакцию №1 Перевод на счет другому лицу, сумма 1001" +
                    "2. Провести транзакцию №2 Перевод на карту, сумма 1001",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time_2.add(Calendar.MINUTE, 22);
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_2));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Превышен период между изменением данных IMSI и первой транзакцией серии");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_2));
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Превышен период между изменением данных IMSI и первой транзакцией серии");
    }

    @Test(
            description = "Клиент №3" +
                    "1. Провести транзакцию №1 Перевод другому лицу" +
                    "2. Провести транзакцию №2 Перевод на карту",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Превышен период между моментом восстановления доступа к ДБО и первой транзакцией серии");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData2
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Превышен период между моментом восстановления доступа к ДБО и первой транзакцией серии");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        return transaction;
    }
}
