package ru.iitdgroup.tests.cases.BIQ_6228;

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
    private final GregorianCalendar time_1 = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final GregorianCalendar time_2 = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 31, 0);



    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 2; i++) {
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
                .getSelect("Перевод на карту другому лицу")
                .tapToSelect()
                .save();

        commandServiceMock.run();
    }

    @Test(
            description = "Клиент №1\n" +
                    "1. Провести транзакцию №1 \"Перевод другому лицу\", сумма 1001\n" +
                    "2. Провести транзакцию №2 \"Перевод на карту\", сумма 1001\n" +
                    "3. Провести транзакцию № 3 Перевод с платежной карты, DocumentSaveTimeStamp больше на 5 минут с момента транзакции № 2, сумма 1001",
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

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData2
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, SCENARIO_BLOCK_TRUE);

        time_1.add(Calendar.MINUTE,6);
        Transaction transaction3 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData3 = transaction3.getData().getTransactionData()
                .withRegular(false);
        transactionData3
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData3
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction3);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Клиент №2\n" +
                    "1. Провести транзакцию №1 \"Перевод другому лицу\", сумма 1001\n" +
                    "2. Провести транзакцию №2 \"Перевод на карту\", сумма 1001",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time_2.add(Calendar.MINUTE,35);
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MAX_PERIOD_BETWEEN_PASSWORD_RECOVERY_FIRST_TRANSACTION);

        Transaction transaction2 = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData2
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        transactionData2
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_2));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MAX_PERIOD_BETWEEN_PASSWORD_RECOVERY_FIRST_TRANSACTION);
    }

    @Test(
            description = "Клиент №3\n" +
                    "1. Провести транзакцию №1 \"Перевод другому лицу\" \n" +
                    "2. Провести транзакцию №2 \"Перевод на карту\"",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1001));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
//        TODO требуется перепроверить после исправления дефекта с правилом GR_99
    }


    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step3"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
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
