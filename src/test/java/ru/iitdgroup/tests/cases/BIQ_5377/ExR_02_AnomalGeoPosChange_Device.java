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

public class ExR_02_AnomalGeoPosChange_Device extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_ExR_02_AnomalGeoPosChange";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Зуля", "Закирова", "Муратовна"}};
    private static final String IP_MOSCOOW = "77.51.50.211";

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
                .fillInputText("Значение:", "0")//Выключить интеграцию с ГИС
                .save();
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
            description = "Провести транзакцию № 1 с указанием ip-адреса",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применяется, в системе выключена проверка GIS_SYSTEM_GIS");
    }

    @Test(
            description = "Включить интеграцию с ГИС (GisSystem_GIS)" +
                    "Провести транзакцию № 2 с указанием ip-адреса, регулярная",
            dependsOnMethods = "step1"
    )
    public void step2() {

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "GisSystem_GIS")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().close();

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применяется для регулярных транзакций");
    }

    @Test(
            description = "Провести транзакцию № 3 без указания ip-адреса",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientDevice()
                .getIOS()
                .withIpAddress(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Нет данных об ip адресе");
    }

    @Test(
            description = "Провести транзакцию № 4 без контейнера устройства",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withClientDevice(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Нет устройств");
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step4"
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