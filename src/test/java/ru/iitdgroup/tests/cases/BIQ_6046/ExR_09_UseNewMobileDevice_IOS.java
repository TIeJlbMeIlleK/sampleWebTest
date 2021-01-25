package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.*;

public class ExR_09_UseNewMobileDevice_IOS extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String REFERENCE_ITEM1 = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные устройства для клиента";
    private static final String IFV1 = new RandomString(15).nextString();
    private static final String DFP1 = new RandomString(15).nextString();
    private static final String IFV2 = new RandomString(15).nextString();
    private static final String DFP2 = new RandomString(15).nextString();
    private static final String IFV3 = new RandomString(15).nextString();
    private static final String DFP3 = new RandomString(15).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String LOGIN = new RandomString(5).nextString();


    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Создаем клиента"

    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH);
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withFirstName("Наталья")
                        .withLastName("Иванова")
                        .withMiddleName("Ильинична")
                        .withLogin(LOGIN)
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test(
            description = "EXR_09 включено/По умолчанию включены флаги в настройке правила" +
                    " \"Использовать информацию из ВЭС\" и \"Использовать информацию из САФ\"",
            dependsOnMethods = "addClient"
    )
    public void enableRules() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Использовать информацию из ВЭС:", true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save();
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);
    }


    @Test(
            description = "Включена IntegrVES2 (1)",
            dependsOnMethods = "enableRules"
    )
    public void enableVES2() {

        getIC().locateTable(REFERENCE_ITEM1)
                .setTableFilter("Описание", "Equals", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .refreshTable()
                .findRowsBy()
                .match("Описание", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save().sleep(5);
    }

    @Test(
            description = "Занести в доверенные устройство № 1 IFV для клиента № 1",
            dependsOnMethods = "enableVES2"
    )

    public void addTrustedDevice() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillCheckBox("Доверенный:", true)
                .fillInputText("IdentifierForVendor:", IFV1)
                .fillInputText("DeviceFingerPrint:", DFP1)
                .fillUser("Клиент:", clientIds.get(0))
                .save().sleep(5);

    }

    @Test(
            description = "Занести в доверенные устройство № 1 DFP для клиента № 1",
            dependsOnMethods = "addTrustedDevice"
    )

    public void sendResponseFromVES() {

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("login", LOGIN);
            json.put("login_hash", LOGIN_HASH);
            json.put("session_id", DFP1);
            json.put("device_hash", DFP1);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "sendResponseFromVES"
    )

    public void step0() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.withSessionId(DFP1);
        transactionData.getClientDevice().getIOS().withIdentifierForVendor(IFV1);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 2 для клиента № 1",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.withSessionId(DFP1);
        transactionData.getClientDevice().getIOS().withIdentifierForVendor(IFV2);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 2 для клиента № 1",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.withSessionId(DFP2);
        transactionData.getClientDevice().getIOS().withIdentifierForVendor(IFV1);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 1 с устройства № 2 для клиента № 1",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.withSessionId(DFP3);
        transactionData.getClientDevice().getIOS().withIdentifierForVendor(IFV3);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "У клиента новое устройство");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getTransactionData().withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
