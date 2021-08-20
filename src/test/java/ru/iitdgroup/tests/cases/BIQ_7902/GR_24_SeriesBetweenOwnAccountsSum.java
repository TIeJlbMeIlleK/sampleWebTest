package ru.iitdgroup.tests.cases.BIQ_7902;

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

public class GR_24_SeriesBetweenOwnAccountsSum extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_24_SeriesBetweenOwnAccounts";
    private static String transactionID1;
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Зинаида", "Жоркина", "Семеновна"}};
    private final String mask = "408";
    private final String sourceProduct = mask + "01020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationProduct1 = mask + "01020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationProduct2 = mask + "01020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationProduct3 = mask + "01020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationProduct4 = mask + "01020" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

    @Test(
            description = "1. Правило GR_24 включено" +
                    "2. Создан инцидент для правила" +
                    "3. \"Период серии\" 10" +
                    "4. \"Длина серии\" 5" +
                    "5. \"Сумма серии\" 1000" +
                    "6. \"Маска счета\" - 408"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Длина серии:", "5")
                .fillInputText("Сумма серии:", "1000")
                .fillInputText("Период серии в минутах:", "10")
                .save()
                .detachDelGroup()
                .addAttachMask("Маска счёта", mask)
                .sleep(20);
        getIC().close();
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
            description = "Провести транзакции № 1 \"Перевод между счетами\", счет списания 408, сумма 10  " +
                    "(Version = 9907, transactionID = 1)",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionTRANSFER_BETWEEN_ACCOUNTS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9907L);
        transactionData
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withDestinationProduct(destinationProduct1)
                .withSourceProduct(sourceProduct);
        transactionID1 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = " Провести транзакцию № 2 \"Закрытие счета\", сумма 990  " +
                    "(Version = 9908, transactionID = 1)",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransactionCLOSURE_ACCOUNT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionID1)
                .withVersion(9908L);
        transactionData
                .getClosureAccount()
                .withAmountInSourceCurrency(BigDecimal.valueOf(990))
                .withDestinationProduct(destinationProduct2)
                .withSourceProduct(sourceProduct);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Общая сумма транзакций больше допустимой величины");
    }

    @Test(
            description = "Провести транзакцию № 3 \"Закрытие вклада\", сумма 10  " +
                    "(Version = 9907, transactionID = 2)",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionCLOSURE_DEPOSIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9907L);
        transactionData
                .getClosureDeposit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withDestinationProduct(destinationProduct3)
                .withSourceProduct(sourceProduct);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Общая сумма транзакций больше допустимой величины");
    }

    @Test(
            description = "Провести транзакцию № 4 \"Перевод между счетами\", " +
                    "счет списания 408, сумма 10  (Version = 9907, transactionID = 3)",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionTRANSFER_BETWEEN_ACCOUNTS();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(9907L);
        transactionData
                .getTransferBetweenAccounts()
                .withAmountInSourceCurrency(BigDecimal.valueOf(10))
                .withDestinationProduct(destinationProduct4)
                .withSourceProduct(sourceProduct);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Общая сумма транзакций больше допустимой величины");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionTRANSFER_BETWEEN_ACCOUNTS() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCLOSURE_ACCOUNT() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_ACCOUNT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCLOSURE_DEPOSIT() {
        Transaction transaction = getTransaction("testCases/Templates/CLOSURE_DEPOSIT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }
}
