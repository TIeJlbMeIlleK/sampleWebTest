package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.ves.mock.VesMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_09_UseNewMobileDevice_2 extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE= "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 7, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private final VesMock vesMock = VesMock.create().withVesPath("/ves/vesEvent").withVesExtendPath("/ves/vesExtendEvent");

    private String stepNSessionIdForIOC;
    private String stepNSessionIdForAndroid;
    private String iOcDevice;

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        vesMock.run();
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(35);

        getIC().close();

    }
    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    @Test(
            description = "Провести транзакцию № 1 от клиента № 1 с iOS с указанием IFV и указанием несуществующего sessionid",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        iOcDevice = transactionData.getClientDevice().getIOS().getIdentifierForVendor();
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
        stepNSessionIdForIOC = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);

    }

    @Test(
            description = "Провести транзакцию № 2 от клиента № 1 с iOS без указания IFV и указанием боевого sessionID",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(null);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
        transactionData.setSessionId(stepNSessionIdForIOC);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);


    }
    @Test(
            description = "Провести транзакцию № 3 от клиента № 1 с Android с указанием IMEI+IMSI и указанием несуществующего sessionid",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        stepNSessionIdForAndroid = transaction.getData().getTransactionData().getSessionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }

    @Test(
            description = "Провести транзакцию № 4 от клиента № 1 с Android без указания IMEI либо IMSI и указанием боевовго sessionid",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(null);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.setSessionId(stepNSessionIdForAndroid);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }

    @Test(
            description = "Провести транзакцию № 5 от клиента № 2 с устройства iOS с указанием IFV устройства № 1 и sessionid устройства № 2",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("8");
        transactionData.getClientDevice().getIOS().setModel("10");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(iOcDevice);
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.1.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
        transaction.getData().getTransactionData().setSessionId(stepNSessionIdForAndroid);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
