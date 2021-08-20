package ru.iitdgroup.tests.cases.BIQ_5377;

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

public class WR_03_EntrustedTransfer extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private static final String REFERENCE_ITEM = "(Rule_tables) Доверенные получатели";
    private static final String TRUSTED_RECIPIENT1 = "Егор Ильич Иванов";
    private static final String TRUSTED_RECIPIENT2 = "Киса Витальевич Емельяненко";
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}, {"Семен", "Рыков", "Петрович"}};

    @Test(
            description = "Включить правило WR_03" +
                    "-- Крупный перевод : 5000" +
                    "-- Период серии в минутах: 10" +
                    "-- Обнуление: 0.95"
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
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
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

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT1)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT2)
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .save();
    }

    @Test(
            description = "Отправить транзакцию №1 от Клиента №1 Платеж по QR-коду через СБП -- Получатель №1, Сумма 5001, остаток 100000",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 Платеж по QR-коду через СБП -- Получатель №1, Сумма 4751, остаток 5000",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(5000))
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(4751));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BIG_AMOUNT);
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 Платеж по QR-коду через СБП -- В транзакции нет Наименования ТСП",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withInitialSourceAmount(BigDecimal.valueOf(100000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(5001))
                .withTSPName(TRUSTED_RECIPIENT1)
                .withTSPType(TRUSTED_RECIPIENT1);
        return transaction;
    }

}
