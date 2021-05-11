package ru.iitdgroup.tests.cases.BIQ_7700_All;

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

public class IR_03_RepeatApprovedTransactionOuter extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String bik = "442301977";
    private final String payeeAccount = "40187200334466554477";
    private final String payeeINN = "312654987123";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ирина", "Муркина", "Сергеевна"}};

    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", true)
                .fillInputText("Длина серии:", "2")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Отклонение суммы (процент 15.04):", "25,55")
                .save()
                .detachWithoutRecording("Типы транзакций")
                .attachIR03SelectAllType()
                .sleep(20);

        getIC().locateTable(REFERENCE_TABLE)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                .select("Наименование канала:", "Мобильный банк")
                .save();
   }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
                String login = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                String loginHash = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(login)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(loginHash)
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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Перевод на счет другому лицу, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету, длину серии, bIK, payeeAccount и payeeINN",
            dependsOnMethods = "addClients"
    )

    public void transOuter() {
        time.add(Calendar.SECOND, 20);
        Transaction transOuter = getOuterTransfer();
        TransactionDataType transactionDataOuter = transOuter.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuter);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод другому лицу», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterOutside = getOuterTransfer();
        TransactionDataType transactionDataOuterOutside = transOuterOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterOutside
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOuterOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBalance = getOuterTransfer();
        TransactionDataType transactionDataOuterAccountBalance = transOuterAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transOuterAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBik = getOuterTransfer();
        TransactionDataType transactionDataOuterAccountBik = transOuterAccountBik.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterAccountBik
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK("442302222");
        sendAndAssert(transOuterAccountBik);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterPayeeAccount = getOuterTransfer();
        TransactionDataType transactionDataOuterPayeeAccount = transOuterPayeeAccount.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterPayeeAccount
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount("40187200334466559988");
        sendAndAssert(transOuterPayeeAccount);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterPayeeINN = getOuterTransfer();
        TransactionDataType transactionDataOuterPayeeINN = transOuterPayeeINN.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterPayeeINN
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeINN("312654988888");
        sendAndAssert(transOuterPayeeINN);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterDeviation = getOuterTransfer();
        TransactionDataType transactionDataOuterDeviation = transOuterDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterDeviation
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод другому лицу» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterLength = getOuterTransfer();
        TransactionDataType transactionDataOuterLength = transOuterLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод другому лицу» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transOuterPeriod = getOuterTransfer();
        TransactionDataType transactionDataOuterPeriod = transOuterPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод другому лицу», условия правила не выполнены");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .getPayeeProps()
                .withPayeeINN(payeeINN)
                .withPayeeAccount(payeeAccount);
        transaction.getData().getTransactionData()
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik);
        return transaction;
    }
}
