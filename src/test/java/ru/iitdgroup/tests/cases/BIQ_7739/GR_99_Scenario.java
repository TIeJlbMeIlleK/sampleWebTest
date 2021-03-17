package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

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
    private static String TABLE_NAME = "(Policy_parameters) Блоки сценариев";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time_1 = new GregorianCalendar(2020, Calendar.DECEMBER, 15, 0, 0, 0);


    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .setPasswordRecoveryDateTime(time_1);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило и выполняем преднастройки",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Период серии в минутах:","5")
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:","30")
                .fillInputText("Логическое выражение:","3&&4")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);

        getIC().locateTable(TABLE_NAME)
                .findRowsBy()
                .match("ID","00003")
                .click()
                .edit()
                .fillInputText("Минимальное количество транзакций в серии:","1")
                .fillInputText("Минимальная сумма всех транзакций серии:","1000")
                .fillInputText("Минимальная сумма транзакции:","1000")
                .getElement("Transaction Type:")
                .getSelect("Перевод другому лицу")
                .tapToSelect()
                .save();
        getIC().locateTable(TABLE_NAME)
                .findRowsBy()
                .match("ID","00004")
                .click()
                .edit()
                .fillInputText("Минимальное количество транзакций в серии:","1")
                .fillInputText("Минимальная сумма всех транзакций серии:","1000")
                .fillInputText("Минимальная сумма транзакции:","1000")
                .getElement("Transaction Type:")
                .getSelect("Заявка на выпуск карты")
                .tapToSelect()
                .save();
        getIC().close();

        commandServiceMock.run();
    }

    @Test(
            description = "Клиент №1\n" +
                    "Провести транзакцию №1 \"Перевод другому лицу\", сумма 1001",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Клиент №1\n" +
                    "Провести транзакцию №2 \"Запрос на выдачу кредита\", сумма 1001",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time_1.add(Calendar.MINUTE,1);
        Transaction transaction2 = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData2
                .getRequestCardIssue()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, SCENARIO_BLOCK_TRUE);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        return transaction;
    }

}
