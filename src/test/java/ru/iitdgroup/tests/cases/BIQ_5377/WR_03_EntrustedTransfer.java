package ru.iitdgroup.tests.cases.BIQ_5377;

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


public class WR_03_EntrustedTransfer extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 23, 14, 10, 0);
    private final List<String> clientIds = new ArrayList<>();

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private static final String REFERENCE_ITEM = "(Rule_tables) Доверенные получатели";

    private static final String TRUSTED_RECIPIENT1 = "Егор Ильич Иванов";
    private static final String TRUSTED_RECIPIENT2 = "Киса Витальевич Емельяненко";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                if (i == 0) {
                    client.getData().getClientData().getClient()
                            .withFirstName("Игорь")
                            .withLastName("Романюк")
                            .withMiddleName("Олегович")
                            .getClientIds()
                            .withDboId(dboId);
                } else {
                    client.getData().getClientData().getClient()
                            .withFirstName("Наталья")
                            .withLastName("Кирова")
                            .withMiddleName("Вячеславовна")
                            .getClientIds()
                            .withDboId(dboId);
                }

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Добавить в Доверенные получателей:\n" +
                    "-- Получатель №1 для Клиента №1\n" +
                    "-- Получатель №2 для Клиента №2",
            dependsOnMethods = "addClient"
    )

    public void addRecipients() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT1)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT2)
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .save()
                .sleep(5);
    }

    @Test(
            description = "Включить правило WR_03\n" +
                    "-- Крупный перевод : 5000\n" +
                    "-- Период серии в минутах: 10\n" +
                    "-- Обнуление: 0.95",
            dependsOnMethods = "addRecipients"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .sleep(10);
    }

    @Test(
            description = "Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1, Сумма 5001, остаток 100000",
            dependsOnMethods = "enableRules"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001))
                .withTSPName(TRUSTED_RECIPIENT1)
                .withTSPType(TRUSTED_RECIPIENT1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(100000));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1, Сумма 4751, остаток 5000",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(4751))
                .withTSPName(TRUSTED_RECIPIENT1)
                .withTSPType(TRUSTED_RECIPIENT1);
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(5000));

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 \"Платеж по QR-коду через СБП\" -- В транзакции нет Наименования ТСП",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В транзакции отсутствует Наименование ТСП. Невозможно идентифицировать получателя");
    }

    @Test(
            description = " Отправить транзакцию №4 от Клиента №2 \"Платеж по QR-коду через СБП\" -- Получатель №2",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TRUSTED_RECIPIENT2)
                .withTSPType(TRUSTED_RECIPIENT2);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);
    }

    @Test(
            description = "Отправить транзакцию №5 от Клиента №2 \"Платеж по QR-коду через СБП\" -- Получатель №3",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200))
                .withTSPName("Петр Иванович Калашников")
                .withTSPType("Петр Иванович Калашников");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось, т.к. отсутствует Наименование ТСП");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
