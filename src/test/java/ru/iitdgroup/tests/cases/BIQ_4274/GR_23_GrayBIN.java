package ru.iitdgroup.tests.cases.BIQ_4274;

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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_23_GrayBIN extends RSHBCaseTest {

    private static final String TABLE_GREY_BIN = "(Rule_tables) Подозрительные банки BIN";
    private static final String RULE_NAME = "R01_GR_23_GrayBIN";
    private static final String BIN_SUSPECT = "427" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 3);
    private static final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    private final String destinationCardNumberSuspect = BIN_SUSPECT + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private final String destinationCardNumber = "43654789" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeePhone = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String payeeAccount = "43654789" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Максимально допустимая сумма:", "1000")
                .save()
                .sleep(25);

        getIC().locateTable(TABLE_GREY_BIN)
                .deleteAll()
                .addRecord()
                .fillInputText("BIN:", BIN_SUSPECT)
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
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
            description = "1. Провести транзакцию № 1 'Перевод по номеру телефона' на подозрительный БИН, сумма 1000",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withDestinationCardNumber(destinationCardNumberSuspect)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAY_BANK);
    }

    @Test(
            description = "2. Провести транзакцию № 2 'Перевод по номеру телефона' не на подозрительный БИН",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getPhoneNumberTransfer()
                .withDestinationCardNumber(destinationCardNumber)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_NOT_GREY_BIN);
    }

    @Test(
            description = "3. Провести транзакцию № 3 'Перевод по номеру телефона' на подозрительный БИН, сумма 10",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getPhoneNumberTransfer()
                .withDestinationCardNumber(destinationCardNumberSuspect)
                .withAmountInSourceCurrency(BigDecimal.valueOf(10.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_MAX_SUM);
    }

    @Test(
            description = "Провести транзакцию № 4 'Перевод по номеру телефон' на подозрительный БИН, сумма 1000, периодический",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getPhoneNumberTransfer()
                .withDestinationCardNumber(destinationCardNumberSuspect)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY_REGULAR);
    }

    @Test(
            description = "Провести транзакцию № 5 'Перевод по номеру телефона' на уникальный номер телефона" +
                    "(без указания 'Номера карты' и 'БИКСЧЕТ')",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeeName(null)
                .withDestinationCardNumber(null)
                .withBIK(null)
                .withPayeeAccount(null)
                .withPayeePhone(payeePhone)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MISSING_CARD_NUMBER);
    }

    @Test(
            description = "Провести транзакцию № 6 'Перевод по номеру телефона' уникальный БИКСЧЕТ" +
                    "(без указания 'Номера карты' и 'Номера телефона')",
            dependsOnMethods = "step5"
    )
    public void step6() {
        time.add(Calendar.MINUTE, 2);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeeName(null)
                .withDestinationCardNumber(null)
                .withBIK(BIK)
                .withPayeeAccount(payeeAccount)
                .withPayeePhone(null)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, MISSING_CARD_NUMBER);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
