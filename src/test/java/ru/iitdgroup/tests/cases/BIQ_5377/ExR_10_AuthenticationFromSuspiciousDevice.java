package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.common.ClientIdsType;
import ru.iitdgroup.intellinx.dbo.transaction.AutoPaymentDataType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_10_AuthenticationFromSuspiciousDevice extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time1;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}, {"Ольга", "Типова", "Ивановна"},
            {"Федор", "Тяпов", "Михайлович"}, {"Иван", "Сидоров", "Петрович"}};
    private String[][] login = {{new RandomString(15).nextString()}};


    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String REFERENCE_ITEM4 = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IMSI = new RandomString(15).nextString();
    private static final String IMEI = new RandomString(15).nextString();
    private static final String DFP = new RandomString(15).nextString();


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 5; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withLogin(login[i][0])
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
            description = "Включить правило R01_ExR_10_AuthenticationFromSuspiciousDevice",
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
            description = "Занести IFV в справочник подозрительных" +
                    "Занести IMSI в справочник подозрительных" +
                    "Занести IMEI в справочник подозрителтьных" +
                    "Занести DFP в справочник подозрительных",
            dependsOnMethods = "enableRules"
    )

    public void addRecipients() {
        Table.Formula ifv = getIC().locateTable(REFERENCE_ITEM1).findRowsBy();
        if (ifv.calcMatchedRows().getTableRowNums().size() > 0) {
            ifv.delete();
        }
        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Identifier for vendor:", IFV)
                .save();

        Table.Formula imsi = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();
        if (imsi.calcMatchedRows().getTableRowNums().size() > 0) {
            imsi.delete();
        }
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillInputText("imsi:", IMSI)
                .save();

        Table.Formula imei = getIC().locateTable(REFERENCE_ITEM3).findRowsBy();
        if (imei.calcMatchedRows().getTableRowNums().size() > 0) {
            imei.delete();
        }
        getIC().locateTable(REFERENCE_ITEM3)
                .addRecord()
                .fillInputText("imei:", IMEI)
                .save();

        Table.Formula dfp = getIC().locateTable(REFERENCE_ITEM4).findRowsBy();
        if (dfp.calcMatchedRows().getTableRowNums().size() > 0) {
            dfp.delete();
        }
        getIC().locateTable(REFERENCE_ITEM4)
                .addRecord()
                .fillInputText("DeviceFingerPrint:", DFP)
                .save();
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "addRecipients"
    )
    public void enableVES() {

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
        getIC().close();
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV и отправить транзакцию",
            dependsOnMethods = "enableVES"
    )

    public void step1() {
        Authentication authentication = getAuthentication();
        authentication
                .getData().getClientAuthentication()
                .getClientIds().setDboId(clientIds.get(0));
        authentication
                .getData().getClientAuthentication()
                .getClientDevice().getIOS()
                .setIdentifierForVendor(IFV);
        sendAndAssert(authentication);

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPlatform(PlatformKind.IOS);
        transactionData.getClientDevice().setIOS(new IOSDevice());
        transactionData.getClientDevice().getIOS().setOSVersion("9");
        transactionData.getClientDevice().getIOS().setModel("12");
        transactionData.getClientDevice().getIOS().setIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.getClientDevice().getIOS().setIpAddress("192.168.10.1");
        transactionData.getClientDevice().getIOS().setAuthByFingerprint(false);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }


    @Test(
            description = "Провести транзакцию №2 от клиента №1 \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "step1"
    )

    public void step2() {
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
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Authentication getAuthentication() {
        Authentication authentication = super.getAuthentication("testCases/Templates/Autentification_Android.xml");
        return authentication;
    }

    private Authentication getAuthenticationIOS() {
        Authentication authentication = super.getAuthentication("testCases/Templates/Autentification_IOS.xml");
        return authentication;
    }

    private Authentication getAuthenticationPC() {
        Authentication authentication = super.getAuthentication("testCases/Templates/Autentification_PC.xml");
        return authentication;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
