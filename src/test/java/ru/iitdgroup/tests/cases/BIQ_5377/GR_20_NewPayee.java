package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_20_NewPayee extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 23, 14, 10, 0);
    private final List<String> clientIds = new ArrayList<>();

    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Карантин получателей";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";

    private static final String TRUSTED_RECIPIENT = "Егор Ильич Иванов";
    private static final String QUARANTINE_RECIPIENT = "Киса Витальевич Емельяненко";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName("Ульяна")
                        .withLastName("Филимонова")
                        .withMiddleName("Витальевна")
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
            description = "Добавить в Карантин получателей -- Получателя №1 для Клиента №1 и " +
                    "Добавить в Доверенные получатели -- Получатель №2 для Клиента №1",
            dependsOnMethods = "addClient"
    )

    public void addRecipients() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM1).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Имя получателя:", QUARANTINE_RECIPIENT)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save()
                .sleep(5);

        Table.Formula rows1 = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }

        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Включить правило GR_20_NewPayee",
            dependsOnMethods = "addRecipients"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(10);
    }

    @Test(
            description = " Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1",
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(200))
                .withTSPName(QUARANTINE_RECIPIENT)
                .withTSPType(QUARANTINE_RECIPIENT);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №2",
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(250))
                .withTSPName(TRUSTED_RECIPIENT)
                .withTSPType(TRUSTED_RECIPIENT);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(20))
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В транзакции отсутствует Наименование ТСП. Невозможно идентифицировать получателя");
    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №3",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200))
                .withTSPName("Петр Иванович Калашников")
                .withTSPType("Петр Иванович Калашников");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
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
