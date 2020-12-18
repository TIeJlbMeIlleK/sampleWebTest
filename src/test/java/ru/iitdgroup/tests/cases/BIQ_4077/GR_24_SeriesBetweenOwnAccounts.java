package ru.iitdgroup.tests.cases.BIQ_4077;

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


public class GR_24_SeriesBetweenOwnAccounts extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_GR_24_SeriesBetweenOwnAccounts";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
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
                        .getClient().withLogin(dboId)
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
            description = "Включаем правило и взводим флаги по клиентам",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
                .fillInputText("Длина серии:","3")
                .fillInputText("Сумма серии:","1000")
                .fillInputText("Период серии в минутах:","10")
                .save()
                .sleep(30);
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "1. Провести транзакции № 1, 2, 3 \"Запрос на выдачу кредита\", сумма 10\n" +
                    "2. Провести транзакцию № 4 \"Запрос на выдачу кредита\", сумма 10\n" +
                    "3. Провести транзакцию № 5 \"Запрос на выдачу кредита\", сумма 10",
            dependsOnMethods = "step0"
    )

    public void step1() {
        for (int i = 0; i<2; i++) {
            Transaction transaction = getTransactionGETTING_CREDIT();
            TransactionDataType transactionData = transaction.getData().getTransactionData()
                    .withRegular(false);
            transactionData
                    .getClientIds()
                    .withDboId(clientIds.get(0));
            transactionData
                    .getGettingCredit()
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));
            sendAndAssert(transaction);
            assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
        }
        for (int i = 0; i<3; i++) {
            Transaction transaction = getTransactionGETTING_CREDIT();
            TransactionDataType transactionData = transaction.getData().getTransactionData()
                    .withRegular(false);
            transactionData
                    .getClientIds()
                    .withDboId(clientIds.get(0));
            transactionData
                    .getGettingCredit()
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));
            sendAndAssert(transaction);
            assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
        }
    }

    @Test(
            description = "5. Провести транзакции № 6 \"Запрос на выдачу кредита\", сумма 10, от Клиента №2",
            dependsOnMethods = "step1"
    )

    public void step2() {
            Transaction transaction = getTransactionGETTING_CREDIT();
            TransactionDataType transactionData = transaction.getData().getTransactionData()
                    .withRegular(false);
            transactionData
                    .getClientIds()
                    .withDboId(clientIds.get(1));
            transactionData
                    .getGettingCredit()
                    .withAmountInSourceCurrency(BigDecimal.valueOf(10));
            sendAndAssert(transaction);
            assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_BY_CONF_GR_25);
    }

    @Test(
            description = "6. Провести транзакцию № 7 \"Запрос на выдачу кредита\", сумма 990, от Клиента №2",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(991));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_SUM);
    }

    @Test(
            description = "6. Провести транзакцию № 7 \"Запрос на выдачу кредита\", сумма 990, от Клиента №2",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_BY_LENGHT);
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
