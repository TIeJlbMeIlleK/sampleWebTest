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

public class IR_03_RepeatApprovedTransactionOuterCardRdakAdak extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String RULE_NAME_ALERT = "R01_ExR_05_GrayIP";
    private static final String REFERENCE_TABLE_RDAK = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_TABLE_ALERT = "(Rule_tables) Подозрительные IP адреса";
    private static final String REFERENCE_TABLE2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_TABLE3 = "(Policy_parameters) Параметры проведения ДАК";

    private final String documentHash = "40817810241000017777";
    private final String ipAddress = "95.73.149.81";
    private String transaction_id;
    private Long version;

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Петр", "Урин", "Семенович"}, {"Ольга", "Румянцева", "Григорьевна"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод с платежной карты стороннего банка на платежную карту РСХБ")
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
                    "Перевод с платежной карты стороннего банка на платежную карту РСХБ, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету",
            dependsOnMethods = "addClients"
    )

    public void transOuterCard() {
        time.add(Calendar.HOUR, -20);
        Transaction transOuterCard = getOuterCardTransfer();
        TransactionDataType transactionDataOuterCard = transOuterCard.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterCard);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterOutside = getOuterCardTransfer();
        TransactionDataType transactionDataOuterOutside = transOuterOutside.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterOutside
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transOuterOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterAccountBalance = getOuterCardTransfer();
        TransactionDataType transactionDataOuterAccountBalance = transOuterAccountBalance.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transOuterAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterDeviation = getOuterCardTransfer();
        TransactionDataType transactionDataOuterDeviation = transOuterDeviation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataOuterDeviation
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transOuterDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Перевод с платежной карты стороннего банка на платежную карту РСХБ» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transOuterLength = getOuterCardTransfer();
        TransactionDataType transactionDataOuterLength = transOuterLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transOuterPeriod = getOuterCardTransfer();
        TransactionDataType transactionDataOuterPeriod = transOuterPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transOuterPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Перевод с платежной карты стороннего банка на платежную карту РСХБ», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getOuterCardTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_CARD_TRANSFER_Android.xml");
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
                .getOuterCardTransfer()
                .withAmountInDestinationCurrency(BigDecimal.valueOf(500.00));
        return transaction;
    }
}
