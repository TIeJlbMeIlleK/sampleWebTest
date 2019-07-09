package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PCDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_06_GrayDevice_NODevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_06_GrayDevice";



    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 4, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);

    }
    @Test(
            description = "Установить значение ожидания ответа от ВЭС меньше значения времени отправки данного ответа",
            dependsOnMethods = "enableRules"
    )
    public void enableVesTimeout() {

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Время ожидания актуальных данных от ВЭС")
                .click()
                .edit()
                .fillInputText("Значение:", "1");

        getIC().close();
    }
    @Test(
            description = "Сгенерировать клиентов",
            dependsOnMethods = "enableVesTimeout"
    )

    public void client() {
        try {
            for (int i = 0; i < 5; i++) {
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
            description = "Выполнить  транзакцию № 1, IFV отсутствует",
            dependsOnMethods = "client"
    )

    public void step0() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .setAndroid(null);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice()
                .setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice()
                .getIOS()
                .setIpAddress("192.178.45.1");
        transactionData.getClientDevice()
                .getIOS()
                .setIdentifierForVendor(null);
        transactionData.getClientDevice()
                .getIOS()
                .setOSVersion("9.1");
        transactionData.getClientDevice()
                .getIOS()
                .setModel("10");
        transactionData.getClientDevice()
                .getIOS()
                .setAuthByFingerprint(false);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }
    @Test(
            description = "Выполнить  транзакцию № 2, IMEI И IMSI отсутствует",
            dependsOnMethods = "step0"
    )
    public void step1() {
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
                .setIMSI(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }

    @Test(
            description = "Выполнить  транзакцию № 2, IMEI отсутствует",
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
                .getClientDevice()
                .getAndroid()
                .setIMEI(null);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("156748541521fd1g165721dfg7");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Выполнить  транзакцию № 3, IMSI отсутствует",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(null);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("56156df7g56156df7fgf165gdf23777723sdf");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Выполнить транзакцию № 4 с несуществующего в ВЭС sessionid (DFP не должно поступить в САФ от ВЭС)",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPC(new PCDevice());
        transactionData.getClientDevice()
                .setPlatform(PlatformKind.PC);
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("192.115.86.15");
        transactionData.getClientDevice()
                .getPC()
                .setUserAgent("123657");
        transactionData.getClientDevice()
                .getPC()
                .setBrowserData("123456");
        transactionData.setSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
    sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_FEW_DATA);
    }

    @Test(
            description = "Провести транзакцию № 5, в транзакции отсутствует контейнер Device",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(4));
        transactionData.setClientDevice(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_DEVICE_NULL);
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
