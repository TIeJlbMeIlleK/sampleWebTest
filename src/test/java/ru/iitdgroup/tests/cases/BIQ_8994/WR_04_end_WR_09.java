package ru.iitdgroup.tests.cases.BIQ_8994;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_04_end_WR_09 extends RSHBCaseTest {

    private static final String RULE_NAME_WR_04 = "R01_WR_04_ToVip";
    private static final String RULE_NAME_WR_09 = "R01_WR_09_NonRiskTSP";
    private static final String RULE_NAME_GRAY_DEVICE_EXR_06 = "R01_ExR_06_GrayDevice";
    private static final String TABLE_VIP_CLIENT_CARD = "(Rule_tables) VIP клиенты НомерКарты";
    private static final String TABLE_NONRISK_TSP = "(Rule_tables) Нерисковые ТСП";
    private static final String TABLE_SUSPECT_IMEI = "(Rule_tables) Подозрительные устройства IMEI";
    private static final String SUSPECT_IMEI = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 15);
    private static final String VIP_CLIENT_CARD = "42786300" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String TSP_NAME = "Андрей";
    private static final String TSP_TYPE = "Индивидуальный Предприниматель";
    private final String[][] names = {{"Анастасия", "Смирнова", "Витальевна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_TYPE_TRANSACTION = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";

    @Test(
            description = "1. Включить правила:" +
                    "-- WR_04_ToVip; В параметр добавлены правила Исключения: ExR_06_GrayDevice" +
                    "-- WR_09_NonRiskTSP" +
                    "-- ExR_06_GrayDevice" +
                    "2. Внести в справочник \"VIP клиенты НомерКарты\" номер карты" +
                    "3. Внести в справочник \"Подозрительные устройства IMEI\" подозрительный IMEI" +
                    "4. Внести в справочник \"Нерисковые ТСП\" нерисковые TSPName и TSPType"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME_WR_04)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .attachPersonalExceptions("ExR_06_GrayDevice")
                .backToAllTheRules()
                .editRule(RULE_NAME_WR_09)
                .fillCheckBox("Active:", true)
                .save()
                .detachWithoutRecording("Персональные Исключения")
                .backToAllTheRules()
                .selectRule(RULE_NAME_GRAY_DEVICE_EXR_06)
                .activate()
                .sleep(10);

        getIC().locateTable(TABLE_VIP_CLIENT_CARD)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер Карты:", VIP_CLIENT_CARD)
                .save();
        getIC().locateTable(TABLE_SUSPECT_IMEI)
                .deleteAll()
                .addRecord()
                .fillInputText("imei:", SUSPECT_IMEI)
                .save();
        getIC().locateTable(TABLE_NONRISK_TSP)
                .deleteAll()
                .addRecord()
                .fillInputText("Нерисковый Тип ТСП или Имя ТСП:", TSP_NAME)
                .select("Тип Нерисковых данных:", "TSP_NAME")
                .save();
        getIC().locateTable(TABLE_NONRISK_TSP)
                .addRecord()
                .fillInputText("Нерисковый Тип ТСП или Имя ТСП:", TSP_TYPE)
                .select("Тип Нерисковых данных:", "TSP_TYPE")
                .save();
        Table rows = getIC().locateTable(TABLE_TYPE_TRANSACTION)
                .setTableFilter("Тип транзакции", "Equals", "Платеж по QR-коду через СБП").refreshTable();
        if (getIC().getDriver().findElementsByXPath("//*[text()='No records were found.']").size() > 0) {
            rows.addRecord()
                    .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                    .select("Наименование канала:", "Мобильный банк").save();
        }
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
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
            description = "1. Отправить Транзакцию №1 Платеж по QR_коду от VIP клиента" +
                    " с подозрительного устройства IMEI и с нерисковым ТСП",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -10);
        Transaction transaction = getTransferQRCode();
        TransactionDataType transactionDataType = transaction.getData().getTransactionData();
        transactionDataType
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1500.00))
                .withTSPName(TSP_NAME)
                .withTSPType(TSP_TYPE)
                .withSourceProduct(VIP_CLIENT_CARD);
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIMEI(SUSPECT_IMEI);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Нерисковый Тип ТСП");
        assertRuleResultForTheLastTransaction(RULE_NAME_WR_04, NOT_TRIGGERED, "Сработало персональное исключение 'ExR_06_GrayDevice' белого правила");
        assertRuleResultForTheLastTransaction(RULE_NAME_GRAY_DEVICE_EXR_06, TRIGGERED, "IMEI найден в сером списке");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME_WR_09;
    }

    private Transaction getTransferQRCode() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0))
                .withLoginHash(clientIds.get(0));
        transactionDataType
                .getPaymentC2B()
                .withSourceProduct(VIP_CLIENT_CARD)
                .withTSPName(TSP_NAME)
                .withTSPType(TSP_TYPE)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1500.00));
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIMEI(SUSPECT_IMEI);
        return transaction;
    }
}
