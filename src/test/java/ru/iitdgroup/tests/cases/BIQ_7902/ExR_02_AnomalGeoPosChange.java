package ru.iitdgroup.tests.cases.BIQ_7902;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//TODO перед запуском теста, должны быть заполнены справочники ГИС

public class ExR_02_AnomalGeoPosChange extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_02_AnomalGeoPosChange";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static String transactionId1;
    private static String transactionId2;

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Зуля", "Закирова", "Муратовна"}};

    @Test(
            description = "Настройка интеграционных параметров и включение правила R01_ExR_02_AnomalGeoPosChange"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "GisSystem_GIS")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().close();
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
            description = "1. Провести транзакцию № 1 с ip-адреса Москвы (Version = 7777, transactionID = 7)77.51.50.211",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.HOUR, -26);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withVersion(7777L)
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("77.51.50.211");
        transactionId1 = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет данных о предыдущей транзакции");
    }

    @Test(
            description = "2. Провести транзакцию № 2 с ip-адреса Токио через 1 секунду " +
                    "(Version = 7778, transactionID = 7)133.250.250.4",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionId1)
                .withVersion(7778L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("133.250.250.4");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 800км/ч");
    }

    @Test(
            description = "3. Провести транзакцию № 3 с ip-адреса Москвы через 24 часа " +
                    "(Version = 7779, transactionID = 7) 77.51.50.211",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.HOUR, 24);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionId1)
                .withVersion(7779L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("77.51.50.211");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "4. Провести транзакцию № 4 с ip-адреса Нижнего Новгорода через 1 секунду " +
                    "(Version = 7779, transactionID = 8)5.164.234.255",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(7779L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("5.164.234.255");
        transactionId2 =transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 400км/ч");
    }

    @Test(
            description = "5. Провести транзакцию № 5 с ip-адреса Дзержинска через 1 секунду " +
                    "(Version = 7780, transactionID = 8) 37.147.196.51",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withTransactionId(transactionId2)
                .withVersion(7780L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("37.147.196.51");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 150км/ч");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}