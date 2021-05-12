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

public class IR_03_RepeatApprovedTransactionOuterCommunal extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String unifiedAccountNumber = "884488";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Сергей", "Глызин", "Витальевич"}};

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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Транзакция ЖКХ, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету, длину серии, bIK, payeeAccount и payeeINN",
            dependsOnMethods = "addClients"
    )

    public void transOuterCommunal() {
        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunal = getOuterCommunalTransfer();
        transOuterCommunal.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterCommunal);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Транзакция ЖКХ», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunalOutside = getOuterCommunalTransfer();
        TransactionDataType transactionDataOuterCommunalOutside = transOuterCommunalOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterCommunalOutside
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOuterCommunalOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunalAccountBalance = getOuterCommunalTransfer();
        TransactionDataType transactionDataOuterCommunalAccountBalance = transOuterCommunalAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterCommunalAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transOuterCommunalAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunalUnifiedAccountNumber = getOuterCommunalTransfer();
        TransactionDataType transactionDataOuterCommunalUnifiedAccountNumber = transOuterCommunalUnifiedAccountNumber.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterCommunalUnifiedAccountNumber
                .getOuterTransfer()
                .getCommunalPaymentProps()
                .withUnifiedAccountNumber("555");
        sendAndAssert(transOuterCommunalUnifiedAccountNumber);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunalDeviation = getOuterCommunalTransfer();
        TransactionDataType transactionDataOuterCommunalDeviation = transOuterCommunalDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterCommunalDeviation
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterCommunalDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Транзакция ЖКХ» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterCommunalLength = getOuterCommunalTransfer();
        transOuterCommunalLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterCommunalLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Транзакция ЖКХ» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transOuterCommunalPeriod = getOuterCommunalTransfer();
        transOuterCommunalPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterCommunalPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Транзакция ЖКХ», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getOuterCommunalTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/COMMUNAL_PAYMENT_Android.xml");
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
                .withIsCommunalPayment(true)
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .getCommunalPaymentProps().withUnifiedAccountNumber(unifiedAccountNumber);
        return transaction;
    }
}
