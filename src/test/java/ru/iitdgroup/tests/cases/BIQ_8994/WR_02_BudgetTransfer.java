package ru.iitdgroup.tests.cases.BIQ_8994;

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

public class WR_02_BudgetTransfer extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_02_BudgetTransfer";
    private static final String RULE_NAME_AttentionClient = "R01_ExR_08_AttentionClient";
    private static final String TABLE_Special_attention = "(Rule_tables) Список клиентов с пометкой особое внимание";
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}, {"Сергей", "Кириллов", "Тимурович"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "1. Включить ExR_08_AttentionClient (правило исключение)" +
                    "2. Включить WR_02_BudgetTransfer (Белое правило)"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_AttentionClient)
                .activate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .sleep(20);
    }

    @Test(
            description = "Создаем клиента" +
                    "3. У Клиента № 1 взведен флаг Особое внимание и он занесен в в справочник \"Список клиентов с пометкой особое внимание\"",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
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

        getIC().locateTable(TABLE_Special_attention)
                .deleteAll()
                .addRecord()
                .fillCheckBox("Признак «Особое внимание»:", true)
                .fillUser("Клиент:", clientIds.get(0))
                .save();
    }

    @Test(
            description = "Отправить Транзакцию №1 в Сторону государства от клиента №1" +
                    "3. Добавить в Белые правила : Персональное Исключение :" +
                    "-- ExR_08_AttentionClient в WR_02_BudgetTransfer;",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionBudgetTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Транзакция в сторону государства");

        getIC().locateRules()
                .editRule(RULE_NAME)
                .save()
                .attachPersonalExceptions("ExR_08_AttentionClient")
                .sleep(25);
    }

    @Test(
            description = "4. Отправить от клиента №1 транзакцию №3 в сторону государства",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 11);
        Transaction transaction = getTransactionBudgetTransfer();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сработало персональное исключение 'ExR_08_AttentionClient' белого правила");
        assertRuleResultForTheLastTransaction(RULE_NAME_AttentionClient, TRIGGERED, "Клиент с пометкой Особое внимание");
    }

    @Test(
            description = "5. Отправить от клиента №2 транзакцию №4 в сторону государства",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 10);
        Transaction transaction = getTransactionBudgetTransfer();
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Транзакция в сторону государства");
    }

    @Override
    protected String getRuleName() { return RULE_NAME; }

    private Transaction getTransactionBudgetTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }
}
