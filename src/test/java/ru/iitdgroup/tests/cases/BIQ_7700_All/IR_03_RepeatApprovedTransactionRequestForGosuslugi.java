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

public class IR_03_RepeatApprovedTransactionRequestForGosuslugi extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String REFERENCE_TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final int gosuslugiRequestType = 15;

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ася", "Кирова", "Георгиевна"}};


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
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Запрос в госуслуги")
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
                    "Запрос в госуслуги, проверить на GosuslugiRequestType",
            dependsOnMethods = "addClients"
    )

    public void requestForGosuslugi() {
        time.add(Calendar.HOUR, -10);
        Transaction transRequestForGosuslugi = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugi = transRequestForGosuslugi.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugi);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiType = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiType = transRequestForGosuslugiType.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataRequestForGosuslugiType
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(10);
        sendAndAssert(transRequestForGosuslugiType);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiTrigg = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiTrigg = transRequestForGosuslugiTrigg.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiTrigg);
        assertLastTransactionRuleApply(TRIGGERED, "Найдена подтвержденная «Запрос в госуслуги» транзакция с совпадающими реквизитами");

        time.add(Calendar.SECOND, 20);
        Transaction transRequestForGosuslugiLength = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiLength = transRequestForGosuslugiLength.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiLength);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Для типа «Запрос в госуслуги» условия правила не выполнены");

        time.add(Calendar.MINUTE, 10);
        Transaction transRequestForGosuslugiPeriod = getTransferRequestForGosuslugi();
        TransactionDataType transactionDataRequestForGosuslugiPeriod = transRequestForGosuslugiPeriod.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transRequestForGosuslugiPeriod);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет подтвержденных транзакций для типа «Запрос в госуслуги», условия правила не выполнены");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferRequestForGosuslugi() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
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
                .getRequestForGosuslugi()
                .withGosuslugiRequestType(gosuslugiRequestType);
        return transaction;
    }
}
