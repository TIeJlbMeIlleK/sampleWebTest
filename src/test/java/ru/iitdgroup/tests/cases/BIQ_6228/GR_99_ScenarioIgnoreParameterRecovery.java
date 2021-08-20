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

public class GR_99_ScenarioIgnoreParameterRecovery extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_99_Scenario";
    private final static String TABLE_NAME = "(Policy_parameters) Блоки сценариев";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Илья", "Птичкин", "Олегович"}, {"Сергей", "Рыков", "Семенович"}};

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void enablerules() {
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
                .fillInputText("Максимальный промежуток времени (смена IMSI):", "")
                .fillInputText("Максимальный промежуток времени (подключение ДБО ФЛ):", "")
                .fillInputText("Максимальный промежуток времени (Смена данных клиента):", "")
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:", "10")
                .fillInputText("Логическое выражение:", ID_BLOK1 + "||" + ID_BLOK2)
                .save()
                .sleep(25);
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enablerules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(new XMLGregorianCalendarImpl(time))
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
            description = "Завести клиента: №1, У которого  с момента восстановления доступа к ДБО " +
                    "до первой транзакции прошло не более 30 минут, остальные даты не указаны или превышают параметр" +
                    "1. Провести транзакцию №1 Перевод на счет другому лицу, сумма 1001" +
                    "2. Провести транзакцию №2 Перевод на карту другому лицу, сумма 1001",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionOuterTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Выражение блока сценариев ИСТИННО!");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, "Выражение блока сценариев ИСТИННО!");
    }

    @Test(
            description = "3. Провести транзакцию №1 Перевод на счет другому лицу, сумма 500" +
                    "4. Провести транзакцию №2 Перевод на карту другому лицу, сумма 500",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData();
        transactionDataType
                .getClientIds().withDboId(clientIds.get(1));
        transactionDataType
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction2.getData().getTransactionData();
        transactionData
                .getClientIds().withDboId(clientIds.get(1));
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось");
    }

    @Test(
            description = "1. Провести транзакцию №1 Перевод на счет другому лицу, сумма 1001" +
                    "2. Провести транзакцию №2 Перевод на карту другому лицу, сумма 1001",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionOuterTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Выражение блока сценариев ИСТИННО!");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, "Выражение блока сценариев ИСТИННО!");
    }

    @Test(
            description = "1. Провести транзакцию №1 Перевод на счет другому лицу, сумма 1001" +
                    "2. Провести транзакцию №2 Перевод на карту другому лицу, сумма 1001",
            dependsOnMethods = "step3"
    )

    public void step4() {
        time.add(Calendar.MINUTE,12);
        Transaction transaction = getTransactionOuterTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Превышен период между моментом восстановления доступа к ДБО и первой транзакцией серии");

        Transaction transaction2 = getTransactionCARD_TRANSFER();
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
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataType
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionDataType
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        return transaction;
    }
}
