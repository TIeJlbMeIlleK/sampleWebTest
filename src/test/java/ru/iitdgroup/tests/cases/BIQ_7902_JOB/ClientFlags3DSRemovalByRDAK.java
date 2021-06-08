package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ClientFlags3DSRemovalByRDAK extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}};
    private static final String CARD_HOLDER_NAME = "Михалков Степан Михайлович";
    private static final long UNIT_TIME = Instant.now().getEpochSecond();

    private static final String PAN_ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String CARD_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);

    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";

    @Test(
            description = "Создаем клиентов"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "SUCCESS_ALERTS_3DS_PARAMETER")
                .click()
                .edit()
                .fillInputText("Значение:", "3")
                .save();
    }

    @Test(
            description = "1.Передать с КАФ новую карточку клиента №1" +
                    "2. Передать из КАФ нефинансовое событие",
            dependsOnMethods = "addClients"
    )
    public void addClientCAF() {
        try {
            String cafClientResponse = getRabbit()// создание клиента КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafClientQueueName())
                    .getCafClientResponse();
            JSONObject json = new JSONObject(cafClientResponse);
            json.put("clientId", clientIds.get(0));
            json.put("cardholderName", CARD_HOLDER_NAME);
            json.put("cardId", CARD_ID);
            json.put("pan", PAN_ACCOUNT);
            json.put("account", PAN_ACCOUNT);
            String newStr = json.toString();
            getRabbit().setCafClientResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);

            HashMap<String, Object> map = new HashMap<>();//установка в карточке клиента КАФ флага смены IMSI 3DS
            map.put("_3DSFLAG", "1");
            map.put("_3DSFLAG_DATE", Instant.now().minus(5, ChronoUnit.DAYS).toString());
            getDatabase().updateWhere("CAF_CLIENTS", map, "WHERE [DBO_ID] = " + clientIds.get(0));

            String cafAlertResponse = getRabbit()//Отправка события КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", clientIds.get(0));
            js.put("cardId", CARD_ID);
            js.put("pan", PAN_ACCOUNT);
            js.put("cardholderName", CARD_HOLDER_NAME);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.put("transactionId", (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5));
            js.put("rrn", new RandomString(10).nextString());
            js.put("terminalIP", "77.51.50.211");
            String newStr1 = js.toString();
            getRabbit().setCafAlertResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
            getRabbit().close();

        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN_ACCOUNT)
                .runReport()
                .openFirstID()
                .getActions()
                .doAction("Подтверждено")
                .approved();

        getIC().locateReports()//проверка в карточке клиента КАФ наличие флага Смены IMSI 3D-SMS
                .openFolder("Бизнес-сущности")
                .openRecord("Клиенты Альфа")
                .setTableFilterWithActive("clientCards", "Equals", PAN_ACCOUNT)
                .runReport()
                .openFirst();
        assertTableField("Смена IMSI 3D-SMS:", "Yes");

        HashMap<String, Object> map = new HashMap<>();//меняем в БД дату события КАФ last_modified на 4 дня назад
        map.put("last_modified", Instant.now().minus(4, ChronoUnit.DAYS).toString());
        map.put("timestamp", Instant.now().minus(4, ChronoUnit.DAYS).toString());
        getDatabase().updateWhere("CAF_ALERT", map, "WHERE [id] = (SELECT MAX([id]) FROM [CAF_ALERT])");
    }

    @Test(
            description = "Запустить джоб ClientFlags3DSRemovalByRDAK и проверить флаги клиента",
            dependsOnMethods = "addClientCAF"
    )
    public void runJob() {
        getIC().locateJobs()
                .selectJob("ClientFlags3DSRemovalByRDAK")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateReports().openFolder("Бизнес-сущности")//проверка снятия флага Смены IMSI 3D-SMS
                .openRecord("Клиенты Альфа")
                .setTableFilterWithActive("clientCards", "Equals", PAN_ACCOUNT)
                .runReport()
                .openFirst();
        assertTableField("Смена IMSI 3D-SMS:", "No");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}