package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

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
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}, {"Илья", "Пупкин", "Олегович"}, {"Ольга", "Типова", "Ивановна"},
            {"Федор", "Тяпов", "Михайлович"}};

    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Подозрительные устройства IdentifierForVendor";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Подозрительные устройства IMSI";
    private static final String REFERENCE_ITEM3 = "(Rule_tables) Подозрительные устройства IMEI";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String IFV = new RandomString(15).nextString();
    private static final String IFV1 = new RandomString(15).nextString();
    private static final String IMSI = ThreadLocalRandom.current().nextLong(0, 15) + "";
    private static final String IMEI = ThreadLocalRandom.current().nextLong(0, 15) + "";

    private static final String LOGIN_1 = new RandomString(5).nextString();
    private static final String LOGIN_2 = new RandomString(5).nextString();
    private static final String LOGIN_3 = new RandomString(5).nextString();
    private static final String LOGIN_4 = new RandomString(5).nextString();

    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 4; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0,12);
                Client client = new Client("testCases/Templates/client.xml");

                if (i == 0) {
                    client.getData().getClientData().getClient()
                            .withLogin(LOGIN_1);
                } else if (i == 1) {
                    client.getData().getClientData().getClient()
                            .withLogin(LOGIN_2);
                } else if (i == 2) {
                    client.getData().getClientData().getClient()
                            .withLogin(LOGIN_3);
                } else {
                    client.getData().getClientData().getClient()
                            .withLogin(LOGIN_4);
                }
                client.getData()
                        .getClientData()
                        .getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
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
                    "Занести IMEI в справочник подозрителтьных",
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
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного IFV," +
                    "проверить карточку клиента и отправить транзакцию",
            dependsOnMethods = "enableVES"
    )

    public void step1() {
        Authentication authentication = getAuthenticationIOS();
        authentication
                .getData().getClientAuthentication()
                .getClientIds().setDboId(clientIds.get(0));
        authentication
                .getData().getClientAuthentication().withLogin(LOGIN_1)
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
                .withLogin(LOGIN_2)
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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
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
                .withLogin(LOGIN_3)
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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(2));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
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
                .getData().getClientAuthentication().withLogin(LOGIN_4)
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
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(3));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .getClientDevice().getIOS()
                .withIdentifierForVendor(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
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

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionIOS() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
