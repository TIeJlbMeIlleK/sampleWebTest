package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
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

public class ExR_10_AuthenticationFromSuspiciousDevice extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}, {"Ольга", "Типова", "Ивановна"},
            {"Федор", "Тяпов", "Михайлович"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Подозрительные устройства IMEI";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IFV1 = new RandomString(15).nextString();
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);

    @Test(
            description = "Включить правило R01_ExR_10_AuthenticationFromSuspiciousDevice"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Identifier for vendor:", IFV)
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillInputText("imsi:", IMSI)
                .save();
        getIC().locateTable(REFERENCE_ITEM3)
                .addRecord()
                .fillInputText("imei:", IMEI)
                .save();

        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 4; i++) {
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
                        .withLoginHash(dboId)
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
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV," +
                    "проверить карточку клиента и отправить транзакцию",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Authentication authentication = getAuthenticationIOS();
        authentication
                .getData().getClientAuthentication()
                .getClientIds().setDboId(clientIds.get(0));
        authentication
                .getData().getClientAuthentication().withLogin(clientIds.get(0))
                .getClientDevice().getIOS()
                .setIdentifierForVendor(IFV);
        sendAndAssert(authentication);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:", "Yes");


        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice().getIOS()
                .withIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 2 для клиента № 2 с подозрительного IMSI," +
                    "проверить карточку клиента №2 и отправить транзакцию №2",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication()
                .getClientIds().setDboId(clientIds.get(1));
        authentication
                .getData().getClientAuthentication()
                .withLogin(clientIds.get(1))
                .getClientDevice().getAndroid()
                .withIMSI(IMSI);
        sendAndAssert(authentication);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(1))
                .runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:", "Yes");


        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMSI(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 3 для клиента № 3 с подозрительного IMEI," +
                    "проверить карточку клиента №3 и отправить транзакцию №3",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication()
                .getClientIds()
                .setDboId(clientIds.get(2));
        authentication
                .getData().getClientAuthentication()
                .withLogin(clientIds.get(2))
                .getClientDevice()
                .getAndroid()
                .withIMEI(IMEI);
        sendAndAssert(authentication);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(2))
                .runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:", "Yes");


        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIMEI(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = " Отправить аутентификацию с сессией № 4 для клиента № 4 с другого IFV," +
                    "проверить карточку клиента и отправить транзакцию",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Authentication authentication = getAuthenticationIOS();
        authentication
                .getData().getClientAuthentication()
                .getClientIds().setDboId(clientIds.get(3));
        authentication
                .getData().getClientAuthentication().withLogin(clientIds.get(3))
                .getClientDevice().getIOS()
                .setIdentifierForVendor(IFV1);
        sendAndAssert(authentication);

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(3))
                .runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:", "No");


        Transaction transaction = getTransactionIOS();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getClientDevice().getIOS()
                .withIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
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

    private Authentication getAuthentication() {
        return super.getAuthentication("testCases/Templates/Autentification_Android.xml");
    }

    private Authentication getAuthenticationIOS() {
        return super.getAuthentication("testCases/Templates/Autentification_IOS.xml");
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

    private Transaction getTransactionIOS() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
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
