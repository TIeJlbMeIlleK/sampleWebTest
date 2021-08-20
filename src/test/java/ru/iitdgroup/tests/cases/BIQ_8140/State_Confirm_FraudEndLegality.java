package ru.iitdgroup.tests.cases.BIQ_8140;

import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class State_Confirm_FraudEndLegality extends RSHBCaseTest {
    private final static String RULE_NAME = "";

    private static final String CARD_HOLDER_NAME = "Михалков Степан Михайлович";
    private static final String CARD_HOLDER_NAME2 = "Семенов Илья Иванович";
    private static final long UNIT_TIME = Instant.now().getEpochSecond();
    private static final String[] ACCOUNT = new String[2];
    private static final String[] PHONE = new String[2];
    private static final String[] PAN = new String[2];
    private static final String[] CARD_ID = new String[2];
    private static final String[] ALFA_ID = new String[2];
    private static final String CONFIRMED = "2";//Подтверждено
    private static final String UNCONFIRMED = "0";//Не подтверждено
    private static final String FRAUD = "1";//Мошенничество
    private static final String UNCONFIRMED_FRAUD = "3";//Неподтвержденное мошенничество
    private static final String TRANS_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String TRANS_ID2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String RRN = new RandomString(10).nextString();
    private static final String RRN2 = new RandomString(10).nextString();

    //TODO Созданы Action События КАФ Workflow "Подтвердить мошенничество" и "Подтвердить правомочность"
    //TODO Созданы Статусы «Подтвержденное мошенничество» и «Подтверждена правомочность»

    @Test(
            description = "1. Отправить карточки клиентов КАФ № 1, 2."
    )
    public void addClientsCAF() {
        try {
            for (int i = 0; i < 2; i++) {
                PAN[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
                CARD_ID[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4);
                PHONE[i] = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                ACCOUNT[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
                ALFA_ID[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);

                String cafClientResponse = getRabbit()// создание клиента КАФ с разными статусами карт
                        .getAllQueues()
                        .getQueue(getProps().getRabbitCafClientQueueName())
                        .getCafClientResponse();
                JSONObject json = new JSONObject(cafClientResponse);

                json.put("clientId", ALFA_ID[i]);
                json.put("account", ACCOUNT[i]);
                json.put("3DsecPhone", PHONE[i]);
                json.put("cardStatus", "Open");
                json.put("cardId", CARD_ID[i]);
                json.put("pan", PAN[i]);

                if (i == 0) {
                    json.put("cardholderName", CARD_HOLDER_NAME);
                } else {
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                }

                System.out.println("ID клиента № 1= " + ALFA_ID[i]);
                System.out.println("Аккаунт клиента № 1 = " + ACCOUNT[i]);
                System.out.println("ID карты №" + i + " = " + CARD_ID[i]);
                System.out.println("Номер карты №" + i + " = " + PAN[i]);

                String newStr = json.toString();
                getRabbit().setCafClientResponse(newStr);
                getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);
            }
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
    }

    @Test(
            description = "2. Отправить Алерт  КАФ от 1 клиента:" +
                    "- с уникальными значениями в полях rrn и  transactionId;",
            dependsOnMethods = "addClientsCAF"
    )
    public void alertCaf1() {
        try {
            String cafAlertResponse = getRabbit()//Отправка события КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", ALFA_ID[0]);
            js.put("cardId", CARD_ID[0]);
            js.put("pan", PAN[0]);
            js.put("cardholderName", CARD_HOLDER_NAME);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.put("transactionId", TRANS_ID);
            js.put("rrn", RRN);
            js.put("terminalIP", "77.51.50.211");
            String newStr1 = js.toString();
            getRabbit().setCafAlertResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[0])
                .runReport()
                .openFirstID()
                .getButtonUnconfirmedFraud()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Неподтвержденное мошенничество");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID)
                .openFirst();
        assertTableField("Тип сообщения:", "CCAFTransactionDesision");
        assertTableField("Запрос:", "{\"rrn\":\"" + RRN + "\",\"transactionId\":\"" + TRANS_ID + "\",\"isFraud\":" + UNCONFIRMED_FRAUD + "," +
                "\"comment\":\"Подтверждение неподтвержденного мошенничества события\",\"operatorName\":\"ic_admin\"" +
                ",\"msgtype\":\"saf_alert_decision\"}");

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[0])
                .runReport()
                .openFirstID()
                .getActions()
                .doAction("Подтвердить мошенничество")
                .approved()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Подтвержденное мошенничество");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID)
                .openFirst();
        assertTableField("Тип сообщения:", "CCAFTransactionDesision");
        assertTableField("Запрос:", "{\"rrn\":\"" + RRN + "\",\"transactionId\":\"" + TRANS_ID + "\"," +
                "\"isFraud\":" + UNCONFIRMED_FRAUD + ",\"comment\":\"Подтверждение неподтвержденного мошенничества события\"," +
                "\"operatorName\":\"ic_admin\",\"msgtype\":\"saf_alert_decision\"}");
    }

    @Test(
            description = "2. Отправить Алерт  КАФ от 2 клиента:" +
                    "- с уникальными значениями в полях rrn и  transactionId;",
            dependsOnMethods = "alertCaf1"
    )
    public void alertCaf2() {
        try {
            String cafAlertResponse = getRabbit()//Отправка события КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", ALFA_ID[1]);
            js.put("cardId", CARD_ID[1]);
            js.put("pan", PAN[1]);
            js.put("cardholderName", CARD_HOLDER_NAME2);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.put("transactionId", TRANS_ID2);
            js.put("rrn", RRN2);
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
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[1])
                .runReport()
                .openFirstID()
                .getButtonUnconfirmedFraud()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Неподтвержденное мошенничество");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID2)
                .openFirst();
        assertTableField("Тип сообщения:", "CCAFTransactionDesision");
        assertTableField("Запрос:", "{\"rrn\":\"" + RRN2 + "\",\"transactionId\":\"" + TRANS_ID2 + "\",\"isFraud\":" + UNCONFIRMED_FRAUD + "," +
                "\"comment\":\"Подтверждение неподтвержденного мошенничества события\",\"operatorName\":\"ic_admin\"" +
                ",\"msgtype\":\"saf_alert_decision\"}");

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[1])
                .runReport()
                .openFirstID()
                .getActions()
                .doAction("Подтвердить правомочность")
                .approved()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Подтверждена правомочность");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID2)
                .openFirst();
        assertTableField("Тип сообщения:", "CCAFTransactionDesision");
        assertTableField("Запрос:", "{\"rrn\":\"" + RRN2 + "\",\"transactionId\":\"" + TRANS_ID2 + "\"," +
                "\"isFraud\":" + UNCONFIRMED_FRAUD + ",\"comment\":\"Подтверждение неподтвержденного мошенничества события\"," +
                "\"operatorName\":\"ic_admin\",\"msgtype\":\"saf_alert_decision\"}");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}