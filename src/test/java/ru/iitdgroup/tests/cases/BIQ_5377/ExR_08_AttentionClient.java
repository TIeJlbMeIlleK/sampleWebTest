package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_08_AttentionClient extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}};

    private static final String RULE_NAME = "R01_ExR_08_AttentionClient";
    private static final String REFERENCE_ITEM = "(Rule_tables) Список клиентов с пометкой особое внимание";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String LOGIN_1 = new RandomString(5).nextString();
    private static final String LOGIN_2 = new RandomString(5).nextString();
    private static final String LOGIN_HASH1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN_HASH2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                Client client = new Client("testCases/Templates/client.xml");
                if (i == 0) {
                    client.getData().getClientData().getClient().withLogin(LOGIN_1);
                } else {
                    client.getData().getClientData().getClient().withLogin(LOGIN_2);
                }

                if (i == 0) {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH1);
                } else {
                    client.getData().getClientData().getClient().getClientIds().withLoginHash(LOGIN_HASH2);
                }
                client.getData()
                        .getClientData()
                        .getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
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
            description = "Включить правило R01_ExR_08_AttentionClient",
            dependsOnMethods = "addClient"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Добавить клиента № 1 в справочник \"Список клиентов с пометкой особое внимание\"" +
                    "и установить флаг \"Признак \"Особое внимание\"." +
                    "Добавить клиента № 2 в справочник \"Список клиентов с пометкой особое внимание\", " +
                    "не устанавливать флаг \"Признак \"Особое внимание\".",
            dependsOnMethods = "enableRules"
    )

    public void addRecipients() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillCheckBox("Признак «Особое внимание»:", true)
                .fillUser("Клиент:", clientIds.get(0))
                .save();

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillCheckBox("Признак «Особое внимание»:", false)
                .fillUser("Клиент:", clientIds.get(1))
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 1от имени клиента № 1 ",
            dependsOnMethods = "addRecipients"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ATTENTION_CLIENT);
    }

    @Test(
            description = "Провести транзакцию № 2 от имени клиента №2",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
