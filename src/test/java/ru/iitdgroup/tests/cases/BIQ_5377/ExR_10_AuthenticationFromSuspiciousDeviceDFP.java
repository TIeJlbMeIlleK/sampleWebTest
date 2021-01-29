package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
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

public class ExR_10_AuthenticationFromSuspiciousDeviceDFP extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Иван", "Сидоров", "Петрович"}};

    private static final String RULE_NAME = "R01_ExR_10_AuthenticationFromSuspiciousDevice";
    private static final String TABLE = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM = "(Rule_tables) Подозрительные устройства DeviceFingerPrint";

    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private static final String DFP = new RandomString(15).nextString();
    private static final String LOGIN = new RandomString(7).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);


    @Test(
            description = "Создаем клиента"
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
                        .withLogin(LOGIN)
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
            description = "Занести DFP в справочник подозрительных",
            dependsOnMethods = "enableRules"
    )

    public void addRecipients() {
        Table.Formula dfp = getIC().locateTable(REFERENCE_ITEM).findRowsBy();
        if (dfp.calcMatchedRows().getTableRowNums().size() > 0) {
            dfp.delete();
        }
        getIC().locateTable(REFERENCE_ITEM)
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
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Отправить аутентификацию с сессией № 1 для клиента № 1 с подозрительного DFP," +
                    "(отправка сообщения с Раббита), проверить карточку клиента и отправить транзакцию",
            dependsOnMethods = "enableVES"
    )

    public void sendResponseFromVES() {
//        try { //нужно перепроверить
//            String vesResponse = getRabbit().getVesResponse();
//            JSONObject json = new JSONObject(vesResponse);
//            json.put("login", LOGIN);
//            json.put("login_hash", LOGIN_HASH);
//            json.put("session_id", DFP);
//            json.put("device_hash", DFP);
//            String newStr = json.toString();
//            getRabbit().setVesResponse(newStr);
//            getRabbit().sendMessage();
//            getRabbit().close();
//        } catch (JSONException e) {
//            throw new IllegalStateException();
//        }

        getRabbit().setVesResponse(getRabbit().getVesResponse()
                .replaceAll("ilushka305", clientIds.get(0))
                .replaceAll("305", clientIds.get(0))
                .replaceAll("dfgjnsdfgnfdkjsgnlfdgfdhkjdf", DFP));
        getRabbit().sendMessage();
        getRabbit().close();

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
        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Authentication getAuthenticationPC() {
        Authentication authentication = super.getAuthentication("testCases/Templates/Autentification_PC.xml");
        return authentication;
    }

    private Transaction getTransactionPC() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
