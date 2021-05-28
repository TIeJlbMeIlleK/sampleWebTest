package ru.iitdgroup.tests.cases.BIQ_7700_All;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class IR_03_RepeatApprovedTransactionCancellation extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final Long transactionIdToCancel = ThreadLocalRandom.current().nextLong(100000000000L);

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Иван", "Рогов", "Дмитриевич"}};


    @Test(
            description = "Включаем правило"
    )

    public void enableRules() {
        System.out.println(transactionIdToCancel);
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("АДАК выполнен:", false)
                .fillCheckBox("РДАК выполнен:", false)
                .fillCheckBox("Требовать совпадения остатка на счете:", false)
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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Отмена операции")
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
                    "Отмена операции, проверить на transactionIdToCancel",
            dependsOnMethods = "addClients"
    )

    public void transactionCancellation() {
        time.add(Calendar.HOUR, -10);
        Transaction transCancellation = getTransCancellation();
        transCancellation.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transCancellation);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Отмена операции», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationtransactionIdToCancel = getTransCancellation();
        TransactionDataType transactionDataCancellation = transCancellationtransactionIdToCancel.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataCancellation
                .getTransactionCancellation()
                .withTransactionIdToCancel(3232323232L);
        sendAndAssert(transCancellationtransactionIdToCancel);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Отмена операции» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationTrigg = getTransCancellation();
        transCancellationTrigg.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transCancellationTrigg);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Отмена операции» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transCancellationLength = getTransCancellation();
        transCancellationLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transCancellationLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Отмена операции» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transCancellationPeriod = getTransCancellation();
        transCancellationPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transCancellationPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Отмена операции», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransCancellation() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSACTION_CANCELLATION_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .getTransactionCancellation()
                .withTransactionIdToCancel(transactionIdToCancel);
        return transaction;
    }
}
