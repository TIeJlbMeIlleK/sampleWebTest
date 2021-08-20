package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class IR_01_CheckDBO extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Кука", "Пашкин", "Семенович"}};
    private static final String RULE_NAME = "R01_IR_01_CheckDBO";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String TYPE_TSP = new RandomString(8).nextString();

    @Test(
            description = "1. В справочник \"Типы транзакци\" заведены - \"Платеж по QR-коду через СБП\"" +
                    "2. В справочник \"Каналы ДБО\" заведены каналы INTERNET_CLIENT и MOBILE_BANK" +
                    "3. В справочник \"Проверяемые типы транзакций и каналы ДБО\" занести запись для \"Интернет банк\" " +
                    "и \"Платеж по QR-коду через СБП\"" +
                    "Включено правило IR_01_CheckDBO"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .select("Наименование канала:", "Интернет клиент")
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
            description = "Провести транзакцию № 1 из \"Мобильный банк\" типа \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "addClient"
    )

    public void step1() {

        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Непроверяемая пара канал ДБО - тип транзакции");
    }

    @Test(
            description = " Провести транзакцию № 2 из \"Интернет банк\" типа \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "step1"
    )

    public void step2() {

        Transaction transaction = getTransactionPC();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Проверяемая пара канал ДБО - тип транзакции");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPC() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        return transaction;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TYPE_TSP)
                .withTSPType(TYPE_TSP);
        return transaction;
    }
}
