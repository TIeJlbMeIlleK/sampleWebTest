package ru.iitdgroup.tests.cases.BIQ_6228;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_99_ScenarioIMSI extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_99_Scenario";
    private static final String TABLE_NAME = "(Policy_parameters) Блоки сценариев";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private final List<String> clientIds = new ArrayList<>();

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
                    .getElement("Transaction Type:")
                    .getSelect("Перевод на счет другому лицу")
                    .tapToSelect()
                    .save();
        }

        ID_BLOK1 = copyThisLine("ID:");
        int count1 = table
                .findRowsBy()
                .match("Transaction Type", "Заявка на выпуск карты")
                .countMatchedRows();

        if (count1 == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Заявка на выпуск карты")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Заявка на выпуск карты")
                    .click()
                    .edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .getElement("Transaction Type:")
                    .getSelect("Заявка на выпуск карты")
                    .tapToSelect()
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
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:", "")
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

        commandServiceMock.run();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
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

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsClient("Изменен IMSI аутент_true");
    }

    @Test(
            description = "Провести транзакцию №1 от клиента 1 - Перевод по номеру телефона, сумма 2000",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (Непроверяемый тип транзакции)");
    }

    @Test(
            description = "2. Провести транзакцию №3 - Перевод на счет другому лицу. сумма 2000р",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionOuterTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось");
    }

    @Test(
            description = "3. Провести транзакцию №2 от клиента - Заявка на выпуск карты. сумма 2000р",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction2 = getTransactionREQUEST_CARD_ISSUE();
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, "Выражение блока сценариев ИСТИННО!");

        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_PC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2000));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2000));
        return transaction;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(2000));
        return transaction;
    }
}
