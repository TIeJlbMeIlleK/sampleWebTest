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

public class IR_03_RepeatApprovedTransactionMTSystemEdit extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private final String editiingTransactionId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String receiverName = "Иванов Иван Иванович";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Альберт", "Свиридов", "Петрович"}};

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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Изменение перевода, отправленного через систему денежных переводов")
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
            description = "1. Провести транзакции для клиента №1, тип транзакции:" +
                    "Изменение перевода, отправленного через систему денежных переводов, проверить на отклонение суммы в пределах 25,5%," +
                    "на совпадение остатка по счету, EditingTransactionId, receiverCountry",
            dependsOnMethods = "addClients"
    )

    public void transferMTEdit() {
        time.add(Calendar.HOUR, -20);
        Transaction transferMTTransferEdit = getMTTransferEdit();
        sendAndAssert(transferMTTransferEdit);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Изменение перевода, отправленного через систему денежных переводов», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditOutside = getMTTransferEdit();
        TransactionDataType transactionDataMTTransferEditOutside = transMTTransferEditOutside.getData().getTransactionData();
        transactionDataMTTransferEditOutside
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(800.00));
        sendAndAssert(transMTTransferEditOutside);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditAccountBalance = getMTTransferEdit();
        TransactionDataType transactionDataMTTransferEditAccountBalance = transMTTransferEditAccountBalance.getData().getTransactionData();
        transactionDataMTTransferEditAccountBalance
                .withInitialSourceAmount(BigDecimal.valueOf(8000.00));
        sendAndAssert(transMTTransferEditAccountBalance);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditReceiverCountry = getMTTransferEdit();
        TransactionDataType transactionDataMTTransferEditReceiverCountry = transMTTransferEditReceiverCountry.getData().getTransactionData();
        transactionDataMTTransferEditReceiverCountry
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withReceiverCountry("Россия");
        sendAndAssert(transMTTransferEditReceiverCountry);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditEditiingTransactionId = getMTTransferEdit();
        TransactionDataType transactionDataMTTransferEditeditiingTransactionId = transMTTransferEditEditiingTransactionId.getData().getTransactionData();
        transactionDataMTTransferEditeditiingTransactionId
                .getMTTransferEdit()
                .withEditingTransactionId("6363");
        sendAndAssert(transMTTransferEditEditiingTransactionId);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditDeviation = getMTTransferEdit();
        TransactionDataType transactionDataMTTransferEditDeviation = transMTTransferEditDeviation.getData().getTransactionData();
        transactionDataMTTransferEditDeviation
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(372.25));
        sendAndAssert(transMTTransferEditDeviation);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Изменение перевода, отправленного через систему денежных переводов» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transMTTransferEditLength = getMTTransferEdit();
        sendAndAssert(transMTTransferEditLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Изменение перевода, отправленного через систему денежных переводов» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transMTTransferEditPeriod = getMTTransferEdit();
        sendAndAssert(transMTTransferEditPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Изменение перевода, отправленного через систему денежных переводов», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getMTTransferEdit() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getMTTransferEdit()
                .withEditingTransactionId(editiingTransactionId)
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00))
                .withReceiverName(receiverName)
                .withReceiverCountry("Российская Федерация");
        return transaction;
    }
}
