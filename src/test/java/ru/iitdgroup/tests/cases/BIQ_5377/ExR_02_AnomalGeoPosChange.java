package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
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
    private static final String IP_MOSCOOW = "77.51.50.211";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Зуля", "Закирова", "Муратовна"}};

    @Test(
            description = "Настройка интеграционных параметров и включение правила R01_ExR_02_AnomalGeoPosChange"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "GisSystem_GIS")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().close();
        commandServiceMock.run();
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
            description = "1. Провести транзакцию № 1 с ip-адреса Москвы 77.51.50.211",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.HOUR, -50);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет данных о предыдущей транзакции");
    }

    @Test(
            description = "2. Провести транзакцию № 2 с ip-адреса Токио через 1 секунду 133.250.250.4",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("133.250.250.4");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 800км/ч");
    }

    @Test(
            description = "3. Провести транзакцию № 3 с ip-адреса Москвы через 24 часа 77.51.50.211  (422км)",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.HOUR, 24);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "4. Провести транзакцию № 4 с ip-адреса Нижнего Новгорода через 1 секунду 5.164.234.255",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("5.164.234.255");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 400км/ч");
    }

    @Test(
            description = "5. Провести транзакцию № 5 с ip-адреса Москвы спустя 24 часа  77.51.50.211",
            dependsOnMethods = "step4"
    )
    public void step5() {
        time.add(Calendar.HOUR, 24);
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "6. Провести транзакцию № 6 с ip-адреса Владимира через 1 секунду 109.229.254.103",
            dependsOnMethods = "step5"
    )
    public void step6() {
        time.add(Calendar.SECOND, 1);
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress("109.229.254.103");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Растояние/время между транзакциями превышает 150км/ч");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step6"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds().withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(IP_MOSCOOW);
        return transaction;
    }
}