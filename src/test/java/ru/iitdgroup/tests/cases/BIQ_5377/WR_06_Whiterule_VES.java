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
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class WR_06_Whiterule_VES extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_WR_06_VES";
    private static String TABLE_NAME = "(System_parameters) Интеграционные параметры";

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String SESSION_ID = new RandomString(13).nextString();

    private static final String COD_ANSWER = "22";

    @Test(
            description = "Включаем правило и настраиваем справочники"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Коды ответов ВЭС")
                .attach("Коды ответов ВЭС", "Идентификатор кода", "Equals", COD_ANSWER)
                .edit()
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .save()
                .sleep(5);

        getIC().locateTable(TABLE_NAME)
                .findRowsBy()
                .match("Код значения", "VES_TIMEOUT")
                .click().edit()
                .fillInputText("Значение:", "600").save();
        getIC().locateTable(TABLE_NAME)
                .findRowsBy()
                .match("Код значения", "IntegrVES2")
                .click().edit()
                .fillInputText("Значение:", "1").save();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "Подготовить ответ от ВЭС, в котором код ответа  будет соответствовать значению по справочнику \"Коды ответов ВЭС\"",
            dependsOnMethods = "addClients"
    )
    public void addClientCAF() {
        try {
            String vesResponse = getRabbit().getVesResponse();
            JSONObject js = new JSONObject(vesResponse);
            js.put("type_id", COD_ANSWER);
            js.put("customer_id", clientIds.get(0));
            js.put("login_hash", LOGIN_HASH);
            js.put("login", LOGIN);
            js.put("session_id", SESSION_ID);
            js.put("device_hash", SESSION_ID);
            String newStr1 = js.toString();
            getRabbit().setVesResponse(newStr1);
            getRabbit().sendMessage();
            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "Отправить транзакцию №1 \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "addClientCAF"
    )

    public void transaction1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withSessionId(SESSION_ID)
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "ВЭС не обнаружил опасных воздействий на устройство");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_IOS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
