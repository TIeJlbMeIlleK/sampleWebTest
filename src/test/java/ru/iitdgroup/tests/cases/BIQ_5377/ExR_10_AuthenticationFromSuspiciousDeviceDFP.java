package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_10_AuthenticationFromSuspiciousDeviceDFP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Иван", "Сидоров", "Петрович"}};

    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String DFP = new RandomString(15).nextString();

    @Test(
            description = "Включить правило R01_ExR_10_AuthenticationFromSuspiciousDevice"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillInputText("DeviceFingerPrint:", DFP)
                .save();
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
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
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                Client client = new Client("testCases/Templates/client.xml");
                client.getData()
                        .getClientData()
                        .getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withLogin(dboId)
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
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного DFP," +
                    "(отправка сообщения с Раббита), проверить карточку клиента и отправить транзакцию",
            dependsOnMethods = "addClient"
    )

    public void sendResponseFromVES() {

        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject json = new JSONObject(vesResponse);
            json.put("login", clientIds.get(0));
            json.put("login_hash", clientIds.get(0));
            json.put("session_id", DFP);
            json.put("device_hash", DFP);
            String newStr = json.toString();
            getRabbit().setVesResponse(newStr);
            getRabbit().sendMessage();
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC()
                .locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        assertTableField("Подозрительное устройство:", "Yes");
        getIC().close();

        Transaction transaction = getTransactionPC();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "sendResponseFromVES"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPC() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        return transaction;
    }
}
