package ru.iitdgroup.tests.cases.BIQ_8140;

import net.bytebuddy.utility.RandomString;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class State_Unconfirmed_Fraud_alertDecision extends RSHBCaseTest {
    private final static String RULE_NAME = "";

    private static final String CARD_HOLDER_NAME = "Михалков Степан Михайлович";
    private static final String CARD_HOLDER_NAME2 = "Семенов Илья Иванович";
    private static final String CARD_HOLDER_NAME3 = "Ольгина Зинаида Павловна";
    private static final String CARD_HOLDER_NAME4 = "Семенов николай Федорович";
    private static final long UNIT_TIME = Instant.now().getEpochSecond();
    private static final String[] ACCOUNT = new String[4];
    private static final String[] PHONE = new String[4];
    private static final String[] PAN = new String[4];
    private static final String[] CARD_ID = new String[4];
    private static final String[] ALFA_ID = new String[4];
    private static final String CONFIRMED = "2";//Подтверждено
    private static final String UNCONFIRMED = "0";//Неподтвержденая
    private static final String FRAUD = "1";//Мошенничество
    private static final String UNCONFIRMED_FRAUD = "3";//Неподтвержденное мошенничество
    private static final String TRANS_ID = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String TRANS_ID2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String TRANS_ID3 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String TRANS_ID4 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);
    private static final String RRN = new RandomString(10).nextString();
    private static final String RRN2 = new RandomString(10).nextString();
    private static final String RRN3 = new RandomString(10).nextString();
    private static final String RRN4 = new RandomString(10).nextString();

    //TODO Создан Action (Неподтвержденное мошенничество) в Событие КАФ Workflow для перехода в новый статус "Неподтвержденное мошенничество".
    //TODO В properties должна быть прописана очередь caf.queues.caf_classified=AlertFromCAFClassified.

    @Test(
            description = "1. Отправить карточки клиентов КАФ № 1, 2, 3, 4;"
    )
    public void addClientsCAF() {
        try {
            for (int i = 0; i < 4; i++) {
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
                } else if (i == 1) {
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                } else if (i == 2) {
                    json.put("cardholderName", CARD_HOLDER_NAME3);
                } else {
                    json.put("cardholderName", CARD_HOLDER_NAME4);
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
                    "- с уникальными значениями по полям rrn и  transactionId;" +
                    "3. Отправить сообщение alertDecision по каждому клиенту:" +
                    "- значения из полей rrn и  transactionId взяты из ранее отправленного алерта," +
                    "- в сообщении классификации Алерта в поле isFraud  внести значение:" +
                    "а) По клиенту №1 значение поля isFraud = 0 (Неподтвержденная);",
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
            String newStr = js.toString();
            getRabbit().setCafAlertResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        try {
            String cafAlertDecisionResponse = getRabbit()//Отправка сообщения alertDecision
                    .getAllQueues()
                    .getQueue(getProps().getRabbitAlertFromCAFClassified())
                    .getCafAlertDecisionResponse();
            JSONObject jsn = new JSONObject(cafAlertDecisionResponse);
            jsn.put("rrn", RRN);
            jsn.put("transactionId", TRANS_ID);
            jsn.put("isFraud", UNCONFIRMED);
            jsn.put("comment", "Все заметки");
            jsn.put("operatorName", "ФИО оператора1");
            String newStr1 = jsn.toString();
            getRabbit().setCafAlertDecisionResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_DECISION_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[0])
                .runReport()
                .openFirstID()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Неподтвержденная");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID)
                .openFirst();
        assertTableField("Тип сообщения:", "AlertFromCAFClassified");
        assertTableField("Запрос:", "{\"isFraud\":\"" + UNCONFIRMED + "\",\"comment\":\"Все заметки\",\"msgtype\":\"alertDecision\"," +
                "\"operatorName\":\"ФИО оператора1\",\"transactionId\":\"" + TRANS_ID + "\",\"rrn\":\"" + RRN + "\"}");

    }

    @Test(
            description = "2. Отправить Алерт  КАФ от 2 клиента:" +
                    "- с уникальными значениями по полям rrn и  transactionId;" +
                    "3. Отправить сообщение alertDecision по каждому клиенту:" +
                    "- значения из полей rrn и  transactionId взяты из ранее отправленного алерта," +
                    "- в сообщении классификации Алерта в поле isFraud внести значение:" +
                    "б) По клиенту №2 значение поля isFraud = 1 (Мошенническая);",
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
            String newStr = js.toString();
            getRabbit().setCafAlertResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        try {
            String cafAlertDecisionResponse = getRabbit()//Отправка сообщения alertDecision
                    .getAllQueues()
                    .getQueue(getProps().getRabbitAlertFromCAFClassified())
                    .getCafAlertDecisionResponse();
            JSONObject js = new JSONObject(cafAlertDecisionResponse);
            js.put("rrn", RRN2);
            js.put("transactionId", TRANS_ID2);
            js.put("isFraud", FRAUD);
            js.put("comment", "Все заметки");
            js.put("operatorName", "ФИО оператора2");
            String newStr1 = js.toString();
            getRabbit().setCafAlertDecisionResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_DECISION_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[1])
                .runReport()
                .openFirstID()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Мошеническая");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID2)
                .openFirst();
        assertTableField("Тип сообщения:", "AlertFromCAFClassified");
        assertTableField("Запрос:", "{\"isFraud\":\"" + FRAUD + "\",\"comment\":\"Все заметки\",\"msgtype\":\"alertDecision\"," +
                "\"operatorName\":\"ФИО оператора2\",\"transactionId\":\"" + TRANS_ID2 + "\",\"rrn\":\"" + RRN2 + "\"}");
    }

    @Test(
            description = "2. Отправить Алерт  КАФ от 3 клиента:" +
                    "- с уникальными значениями по полям rrn и  transactionId;" +
                    "3. Отправить сообщение alertDecision по каждому клиенту:" +
                    "- значения из полей rrn и  transactionId взяты из ранее отправленного алерта," +
                    "- в сообщении классификации Алерта в поле isFraud  внести значение:" +
                    "в) от клиента №3 значение поля isFraud = 2 (Подтвержденная);",
            dependsOnMethods = "alertCaf2"
    )
    public void alertCaf3() {
        try {
            String cafAlertResponse = getRabbit()//Отправка события КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", ALFA_ID[2]);
            js.put("cardId", CARD_ID[2]);
            js.put("pan", PAN[2]);
            js.put("cardholderName", CARD_HOLDER_NAME3);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.put("transactionId", TRANS_ID3);
            js.put("rrn", RRN3);
            js.put("terminalIP", "77.51.50.211");
            String newStr = js.toString();
            getRabbit().setCafAlertResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        try {
            String cafAlertDecisionResponse = getRabbit()//Отправка сообщения alertDecision
                    .getAllQueues()
                    .getQueue(getProps().getRabbitAlertFromCAFClassified())
                    .getCafAlertDecisionResponse();
            JSONObject js = new JSONObject(cafAlertDecisionResponse);
            js.put("rrn", RRN3);
            js.put("transactionId", TRANS_ID3);
            js.put("isFraud", CONFIRMED);
            js.put("comment", "Все заметки");
            js.put("operatorName", "ФИО оператора3");
            String newStr1 = js.toString();
            getRabbit().setCafAlertDecisionResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_DECISION_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[2])
                .runReport()
                .openFirstID()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Подтвержденная");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID3)
                .openFirst();
        assertTableField("Тип сообщения:", "AlertFromCAFClassified");
        assertTableField("Запрос:", "{\"isFraud\":\"" + CONFIRMED + "\",\"comment\":\"Все заметки\",\"msgtype\":\"alertDecision\"," +
                "\"operatorName\":\"ФИО оператора3\",\"transactionId\":\"" + TRANS_ID3 + "\",\"rrn\":\"" + RRN3 + "\"}");

    }

    @Test(
            description = "2. Отправить Алерт  КАФ от 4 клиента:" +
                    "- с уникальными значениями по полям rrn и  transactionId;" +
                    "3. Отправить сообщение alertDecision по каждому клиенту:" +
                    "- значения из полей rrn и  transactionId взяты из ранее отправленного алерта," +
                    "- в сообщении классификации Алерта в поле isFraud  внести значение:" +
                    "г) от клиента №4 значение поля isFraud = 3 (Неподтвержденное мошенничество);",
            dependsOnMethods = "alertCaf3"
    )
    public void alertCaf4() {
        try {
            String cafAlertResponse = getRabbit()//Отправка события КАФ
                    .getAllQueues()
                    .getQueue(getProps().getRabbitCafAlertQueueName())
                    .getCafAlertResponse();
            JSONObject js = new JSONObject(cafAlertResponse);

            js.put("ruleTriggerDate", UNIT_TIME);
            js.put("alfaId", ALFA_ID[3]);
            js.put("cardId", CARD_ID[3]);
            js.put("pan", PAN[3]);
            js.put("cardholderName", CARD_HOLDER_NAME4);
            js.put("date", UNIT_TIME);
            js.put("localdate", UNIT_TIME);
            js.put("transactionId", TRANS_ID4);
            js.put("rrn", RRN4);
            js.put("terminalIP", "77.51.50.211");
            String newStr = js.toString();
            getRabbit().setCafAlertResponse(newStr);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_RESPONSE);
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        try {
            String cafAlertDecisionResponse = getRabbit()//Отправка сообщения alertDecision
                    .getAllQueues()
                    .getQueue(getProps().getRabbitAlertFromCAFClassified())
                    .getCafAlertDecisionResponse();
            JSONObject js = new JSONObject(cafAlertDecisionResponse);
            js.put("rrn", RRN4);
            js.put("transactionId", TRANS_ID4);
            js.put("isFraud", UNCONFIRMED_FRAUD);
            js.put("comment", "Все заметки");
            js.put("operatorName", "ФИО оператора4");
            String newStr1 = js.toString();
            getRabbit().setCafAlertDecisionResponse(newStr1);
            getRabbit().sendMessage(Rabbit.ResponseType.CAF_ALERT_DECISION_RESPONSE);
            getRabbit().close();
        } catch (JSONException e) {
            throw new IllegalStateException();
        }

        getIC().locateReports()//подтверждение правомочности события КАФ
                .openCreateReport("Событие КАФ")
                .setTableFilterWithActive("Карта клиента Альфа", "Equals", PAN[3])
                .runReport()
                .openFirstID()
                .sleep(2);
        assertTableField("Статус события:", "Обработано");
        assertTableField("Резолюция события:", "Неподтвержденное мошенничество");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", TRANS_ID4)
                .openFirst();
        assertTableField("Тип сообщения:", "AlertFromCAFClassified");
        assertTableField("Запрос:", "{\"isFraud\":\"" + UNCONFIRMED_FRAUD + "\",\"comment\":\"Все заметки\",\"msgtype\":\"alertDecision\"," +
                "\"operatorName\":\"ФИО оператора4\",\"transactionId\":\"" + TRANS_ID4 + "\",\"rrn\":\"" + RRN4 + "\"}");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}