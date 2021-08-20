package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_05_GrayIP extends RSHBCaseTest {
    //TODO перед запуском автотеста должны быть выполнены job с IP адресами
    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String RULE_NAME = "R01_ExR_05_GrayIP";
    private static final String REFERENCE_ITEM = "(Rule_tables) Подозрительные IP адреса";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IP_ADRES1 = "77.51.50.211";
    private static final String IP_ADRES2 = "212.100.151.218";
    private static final String MASKA_IP = "10.0.0.0/24";
    private static final String MASKA_IP1 = "10.0.0.22";

    @Test(
            description = "Включить правило R01_ExR_05_GrayIP"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillInputText("IP устройства:", IP_ADRES1)
                .fillInputText("Маска подсети устройства:", MASKA_IP)
                .save();
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
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
            description = "Выполнить регулярную транзакцию № 1 с подозрительного устройства IP",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(IP_ADRES1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Выполнить транзакцию № 2 с подозрительного устройства IP",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(IP_ADRES1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IP);
    }

    @Test(
            description = "Выполнить транзакцию № 3 не с подозрительного устройства IP",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(IP_ADRES2);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_NO_GREY_IP);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с подозрительной маски IP_адреса",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(MASKA_IP1);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GREY_IP);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step4"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        return transaction;
    }
}