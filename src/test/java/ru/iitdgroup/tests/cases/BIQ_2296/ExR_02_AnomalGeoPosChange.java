package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_02_AnomalGeoPosChange extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_02_AnomalGeoPosChange";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 4, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .save()
                .sleep(3);

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(3);
    }
    @Test(
            description = "Настройка и включение правила"
    )
    public void editReferenceData() {

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "0").save();

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceData"
    )
    public void step0() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "Провести транзакцию № 1 с указанием ip-адреса",
            dependsOnMethods = "step0"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("192.168.0.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_GIS_SETTING);
    }

    @Test(
            description = "Включить интеграцию с ГИС (GisSystem_GIS)",
            dependsOnMethods = "step1"
    )
    public void step2() {
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Система, геоданные из которой будут использоваться. Если стоит Нет, то используются геоданные ВЭС.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();

        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 2 с указанием ip-адреса, регулярная",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("192.168.0.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION);
    }

    @Test(
            description = "Провести транзакцию № 3 без указания ip-адреса",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, REQUIRE_IP);
    }

    @Test(
            description = "Провести транзакцию № 4 без контейнера устройства",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.setClientDevice(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_DEVICE);
    }
    @Test(
            description = "Провести транзакцию № 5 с несуществующего ip-адреса",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("151.555.555.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, REQUIRE_GEOLOCATION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
