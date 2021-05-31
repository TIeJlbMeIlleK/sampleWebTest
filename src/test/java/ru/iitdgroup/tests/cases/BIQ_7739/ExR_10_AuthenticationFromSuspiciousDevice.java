package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.Assert;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.auth.ClientAuthenticationType;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;
import static org.testng.Assert.assertEquals;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_10_AuthenticationFromSuspiciousDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String Table_DFP = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";
    private static final String Table_IFV = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String Table_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String Table_IMSI = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String DFP = new RandomString(30).nextString();
    private static final String IFV = new RandomString(20).nextString();
    private static final String IMEI = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String IMSI = (ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15);

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(20);
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceTable() {
        getIC().locateTable(Table_DFP)
                .deleteAll()
                .addRecord()
                .fillMasked("DeviceFingerPrint:", DFP)
                .save();
        getIC().locateTable(Table_IFV)
                .deleteAll()
                .addRecord()
                .fillMasked("Identifier for vendor:", IFV)
                .save();
        getIC().locateTable(Table_IMEI)
                .deleteAll()
                .addRecord()
                .fillMasked("imei:", IMEI)
                .save();
        getIC().locateTable(Table_IMSI)
                .deleteAll()
                .addRecord()
                .fillMasked("imsi:", IMSI)
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceTable"
    )
    public void client() {
        try {
            for (int i = 0; i < 5; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                sendAndAssert(client);
                System.out.println(dboId);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV. Отправить транзакцию по данному клиенту",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Authentication authentication = getAuthenticationIOS();
        authentication
                .getData()
                .getClientAuthentication()
                .getClientIds()
                .setDboId(clientIds.get(0));
        authentication
                .getData()
                .getClientAuthentication()
                .getClientDevice()
                .getIOS()
                .withIdentifierForVendor(IFV)
                .withIpAddress("192.168.10.1")
                .withModel("10");
        sendAndAssert(authentication);

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_IOC();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getIOS()
                .withOSVersion("9");
        transactionData
                .getClientDevice()
                .getIOS()
                .withModel("12");
        transactionData.getClientDevice()
                .getIOS()
                .withIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.getClientDevice()
                .getIOS()
                .withIpAddress("192.168.10.1");
        transactionData.getClientDevice()
                .getIOS()
                .withAuthByFingerprint(false);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 2 для клиента № 2 с подозрительного IMSI Отправить транзакцию по данному клиенту",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication().getClientIds().withDboId(clientIds.get(1));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .withIMSI(IMSI)
                .withIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(authentication);

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 3 для клиента № 3 с подозрительного IMEI",
            dependsOnMethods = "transaction2"
    )

    public void transaction3() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication().getClientIds().withDboId(clientIds.get(2));
        authentication.getData().getClientAuthentication().getClientDevice().getAndroid()
                .withIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15))
                .withIMEI(IMEI);
        sendAndAssert(authentication);

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 с подозрительного DFP",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Authentication authentication = getAuthentication();
        ClientAuthenticationType clientAuthentication = authentication.getData().getClientAuthentication();
        clientAuthentication
                .withSessionId(DFP)
                .withLogin(clientIds.get(3))
                .getClientIds()
                .withDboId(clientIds.get(3))
                .withLoginHash(clientIds.get(3))
                .withEksId(clientIds.get(3))
                .withCifId(clientIds.get(3));
        sendAndAssert(authentication);
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46", "46")
                .replaceAll("ilushka305", clientIds.get(3))
                .replaceAll("305", clientIds.get(3))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf", DFP));
        getRabbit().sendMessage();

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(3)).runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:","Yes");
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 4 для клиента № 4 не с подозрительного DFP",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Authentication authentication = getAuthentication();
        ClientAuthenticationType clientAuthentication = authentication.getData().getClientAuthentication();

        clientAuthentication
                .getClientIds()
                .withDboId(clientIds.get(4));
        clientAuthentication.getClientDevice().getAndroid()
                .withIMSI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15))
                .withIMEI((ThreadLocalRandom.current().nextLong(100000000000000L, Long.MAX_VALUE) + "").substring(0, 15));
        String IMEI_rep = clientAuthentication.getClientDevice().getAndroid().getIMEI();
        String IMSI_rep = clientAuthentication.getClientDevice().getAndroid().getIMSI();
        sendAndAssert(authentication);

        Transaction transaction = getTransactionREQUEST_CARD_ISSUE_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData.getClientDevice()
                .getAndroid()
                .withIMSI(IMSI_rep);
        transactionData.getClientDevice()
                .getAndroid()
                .withIMEI(IMEI_rep);
        String sessionID = transactionData.getSessionId();
        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("46", "46")
                .replaceAll("ilushka305", clientIds.get(4))
                .replaceAll("305", clientIds.get(4))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf", sessionID));
        getRabbit().sendMessage();
        getRabbit().close();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Authentication getAuthentication() {
        Authentication authentication = super.getAuthentication("auth/auth1.xml");
        return authentication;
    }

    private Authentication getAuthenticationIOS() {
        Authentication authentication = super.getAuthentication("auth/auth_IOS.xml");
        return authentication;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
