package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class ExR_09_UseNewMobileDevice_Android extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String REFERENCE_ITEM1 = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные устройства для клиента";
    private static final String IMEI1 = "323232323232323232";
    private static final String IMSI1 = "323232323232323232";
    private static final String SIM_NUMBER1 = "323232323232323232";
    private static final String DEVICE_NUMBER1 = "323232323232323232";
    private static final String DeviceFingerPrint = "323232323232323232";
    private static final String IMEI2 = "333333333333333333";
    private static final String IMSI2 = "333333333333333333";
    private static final String DEVICE_NUMBER2 = "333333333333333333";
    private static final String SIM_NUMBER2 = "333333333333333333";
    private static final String IMEI3 = "444444444444444444";
    private static final String IMSI3 = "444444444444444444";
    private static final String DEVICE_NUMBER3 = "444444444444444444";
    private static final String SIM_NUMBER3 = "444444444444444444";


    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Создаем клиента"

    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withFirstName("Ольга")
                        .withLastName("Иванова")
                        .withMiddleName("Петровна")
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
            description = "Занести в доверенные устройства № 1 IMEI+IMSI+Серийный номер SIM+ Серийный номер устройства и DFP для клиента № 1",
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
                .fillInputText("IMEI:", IMEI1)
                .fillInputText("IMSI:", IMSI1)
                .fillInputText("Серийный номер SIM:", SIM_NUMBER1)
                .fillInputText("Серийный номер устройства:", DEVICE_NUMBER1)
                .fillInputText("DeviceFingerPrint:", DeviceFingerPrint)
                .fillUser("Клиент:", clientIds.get(0))
                .save().sleep(5);

    }

    @Test(
            description = "Изменить в устройстве № 1 IMEI (устройство № 3)",
            dependsOnMethods = "step4"
    )

    public void overwriteIMEIinTrustedDevice() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() == 1) {
            rows.click().edit()
                    .fillCheckBox("Доверенный:", true)
                    .fillInputText("IMEI:", IMEI3)
                    .save().sleep(5);
        }
    }

    @Test(
            description = "Изменить в устройстве № 1 IMSI (устройство № 4)",
            dependsOnMethods = "step5"
    )

    public void overwriteIMSIinTrustedDevice() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() == 1) {
            rows.click().edit()
                    .fillCheckBox("Доверенный:", true)
                    .fillInputText("IMSI:", IMSI3)
                    .save().sleep(5);
        }
    }

    @Test(
            description = "Изменить в устройстве № 1 серийный номер устройства (устройство № 5)",
            dependsOnMethods = "step6"
    )

    public void overwriteNumberDeviceInTrustedDevice() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() == 1) {
            rows.click().edit()
                    .fillCheckBox("Доверенный:", true)
                    .fillInputText("Серийный номер устройства:", DEVICE_NUMBER3)
                    .save().sleep(5);
        }
    }

    @Test(
            description = "Изменить в устройстве № 1 серийный номер SIM (устройство № 6)",
            dependsOnMethods = "step7"
    )

    public void overwriteSIMinTrustedDevice() {

        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() == 1) {
            rows.click().edit()
                    .fillCheckBox("Доверенный:", true)
                    .fillInputText("Серийный номер SIM:", SIM_NUMBER3)
                    .save().sleep(5);
        }
        getIC().close();
    }


    @Test(
            description = "Провести транзакцию № 1 с устройства № 1 от клиента № 1",
            dependsOnMethods = "addTrustedDevice"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().setIMEI(IMEI1);
        transactionData.getClientDevice().getAndroid().setIMSI(IMSI1);
        transactionData.getClientDevice().getAndroid().setDeviceID(DEVICE_NUMBER1);
        transactionData.getClientDevice().getAndroid().setSimSerial(SIM_NUMBER1);
        transactionData.getClientDevice().getAndroid().setSerial(DEVICE_NUMBER1);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 2 с устройства № 1 от клиента № 2",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI1);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI1);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER1);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Отсутствуют доверенные устройства");
    }

    @Test(
            description = "Провести транзакцию № 3 с устройства № 2 от клиента № 1",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI2);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI2);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER2);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER2);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER2);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "У клиента новое устройство");
    }

    @Test(
            description = "Провести транзакцию № 4 с устройства № 2 от клиента № 2",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI2);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI2);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER2);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER2);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER2);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Отсутствуют доверенные устройства");
    }

    @Test(
            description = "Провести транзакцию № 5 с устройства № 3 для клиента № 1",
            dependsOnMethods = "overwriteIMEIinTrustedDevice"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI3);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI1);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER1);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 6 с устройства № 4 для клиента № 1",
            dependsOnMethods = "overwriteIMSIinTrustedDevice"
    )

    public void step6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI3);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI3);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER1);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 7 с устройства № 5 для клиента № 1",
            dependsOnMethods = "overwriteNumberDeviceInTrustedDevice"
    )

    public void step7() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI3);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI3);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER1);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER3);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 8 с устройства № 6 для клиента № 1",
            dependsOnMethods = "overwriteSIMinTrustedDevice"
    )

    public void step8() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI3);
        transactionData.getClientDevice().getAndroid().withIMSI(IMSI3);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER3);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 9 с \"серийным номером SIM\" устройства №1(Без остальных полей) для клиента № 1",
            dependsOnMethods = "step8"
    )

    public void step9() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(IMEI3);
        transactionData.getClientDevice().getAndroid().withIMSI(null);
        transactionData.getClientDevice().getAndroid().withDeviceID(null);
        transactionData.getClientDevice().getAndroid().withSimSerial(null);
        transactionData.getClientDevice().getAndroid().withSerial(null);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 10 с \"серийным номером устройства\" " +
                    "по устройству №1(Без остальных полей) для клиента № 1",
            dependsOnMethods = "step9"
    )

    public void step10() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(null);
        transactionData.getClientDevice().getAndroid().withIMSI(null);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSimSerial(null);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER3);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }

    @Test(
            description = "Провести транзакцию № 11 с \"серийным номером устройства\" и " +
                    "\"серийным номером SIM\" по устройству №1(Без IMSI+IMEI) для клиента № 1",
            dependsOnMethods = "step10"
    )

    public void step11() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getAndroid().withIMEI(null);
        transactionData.getClientDevice().getAndroid().withIMSI(null);
        transactionData.getClientDevice().getAndroid().withDeviceID(DEVICE_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSimSerial(SIM_NUMBER3);
        transactionData.getClientDevice().getAndroid().withSerial(DEVICE_NUMBER3);


        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Устройство клиента найдено в списке ранее использовавшихся");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
