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

public class IR_03_RepeatApprovedTransactionGettingCredit extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String destinationProduct = "42563777" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Сергей", "Кириллов", "Олегович"}};


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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос на выдачу кредита")
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
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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
                    "Запрос на выдачу кредита, проверить на destinationProduct",
            dependsOnMethods = "addClients"
    )

    public void gettingCredit() {
        time.add(Calendar.HOUR, -10);
        Transaction transCredit = getTransferGettingCredit();
        sendAndAssert(transCredit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос на выдачу кредита», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreditDestinationProduct = getTransferGettingCredit();
        TransactionDataType transactionDataCreditDestinationProduct = transCreditDestinationProduct.getData().getTransactionData();
        transactionDataCreditDestinationProduct
                .getGettingCredit()
                .withDestinationProduct("42753444" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12));
        sendAndAssert(transCreditDestinationProduct);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreditBalance = getTransferGettingCredit();
        transCreditBalance.getData().getTransactionData()
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transCreditBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreditSum = getTransferGettingCredit();
        TransactionDataType transactionDataCreditSUM = transCreditSum.getData().getTransactionData();
        transactionDataCreditSUM
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transCreditSum);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCreditSumma = getTransferGettingCredit();
        TransactionDataType transactionDataCreditSumma = transCreditSumma.getData().getTransactionData();
        transactionDataCreditSumma
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transCreditSumma);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос на выдачу кредита» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transCreditLength = getTransferGettingCredit();
        sendAndAssert(transCreditLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос на выдачу кредита» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transCreditPeriod = getTransferGettingCredit();
        sendAndAssert(transCreditPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос на выдачу кредита», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferGettingCredit() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withDestinationProduct(destinationProduct);
        return transaction;
    }
}
