package ru.iitdgroup.tests.cases.BIQ_7993;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;
import ru.iitdgroup.tests.webdriver.report.ReportRecord;

import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.assertEquals;

public class Restricted167FZ_Button extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_GR_20_NewPayee";

    private static final String CARD_HOLDER_NAME = "Глызин Юрий Андреевич";
    private static final String CARD_HOLDER_NAME2 = "Тырин Олег Семенович";

    private static final String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
    private static final String dboId2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);

    private static final String ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String ACCOUNT2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

    private static final String PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String PHONE2 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);

    private final static String[] PAN = new String[5];
    private static final String[] CARD_ID = {
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4)};

    private static final String[] CARD_STATUS = {
            "Restricted 167-FZ",
            "Open",
            "Compromised Antifraud",
            "Stolen (voice)",
            "Lost (voice)",
            "K"};

    //TODO перед запуском должен быть создан Action для Клиент Альфа Workflow:
    // -- Display name: Разблокировать все карты 167-ФЗ
    // -- Unique name: massunblock
    // -- В ExternalApi выбрать "(CCAF) Send Card command"
    // Статусная доступность из Any State в Keep Current State


    @Test(
            description = "Создаем клиентов КАФ"
    )
    public void addClientsCAF() {
        try {
            for (int i = 0; i < 5; i++) {
                PAN[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

                String cafClientResponse = getRabbit()// создание клиента КАФ с разными статусами карт
                        .getAllQueues()
                        .getQueue(getProps().getRabbitCafClientQueueName())
                        .getCafClientResponse();
                JSONObject json = new JSONObject(cafClientResponse);

                json.put("cardId", CARD_ID[i]);
                json.put("pan", PAN[i]);

                if (i <= 2) {
                    json.put("clientId", dboId);
                    json.put("cardholderName", CARD_HOLDER_NAME);
                    json.put("account", ACCOUNT);
                    json.put("3DsecPhone", PHONE);
                    json.put("cardStatus", CARD_STATUS[i]);
                    System.out.println("ID клиента № 1= " + dboId);
                    System.out.println("Аккаунт клиента № 1 = " + ACCOUNT);
                    System.out.println("Статус карты №" + i + " = " + CARD_STATUS[i]);
                } else if (i == 3) {
                    json.put("clientId", dboId2);
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[1]);
                    System.out.println("ID клиента № 2 = " + dboId2);
                    System.out.println("Аккаунт клиента № 2 = " + ACCOUNT2);
                    System.out.println("Статус карты №" + i + " = " + CARD_STATUS[1]);
                } else {
                    json.put("clientId", dboId2);
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[5]);
                    System.out.println("ID клиента № 2 = " + dboId2);
                    System.out.println("Аккаунт клиента № 2 = " + ACCOUNT2);
                    System.out.println("Статус карты №" + i + " = " + CARD_STATUS[5]);
                }

                System.out.println("ID карты №" + i + " = " + CARD_ID[i]);
                System.out.println("Номер карты №" + i + " = " + PAN[i]);
                System.out.println("--------------------------------");
                String newStr = json.toString();
                getRabbit().setCafClientResponse(newStr);
                getRabbit().sendMessage(Rabbit.ResponseType.CAF_CLIENT_RESPONSE);
            }
        } catch (JSONException e) {
            throw new IllegalStateException();
        }
        getRabbit().close();
    }

    @Test(
            description = "1. Перейти в карточку клиента КАФ №1 и нажать на кнопку «Разблокировать все карты 167-ФЗ»" +
                    "2. Проверить в логированных сообщениях отправку запроса на разблокировку карт по данному клиенту" +
                    "-- Проверить корректность формирования полей в запросе",
            dependsOnMethods = "addClientsCAF"
    )
    public void clientsCAF1() {

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Клиенты Альфа")
                .setTableFilterWithActive("ИД клиента", "Equals", dboId)
                .runReport()
                .openFirst()
                .getActionsClientButton167FZ();

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", CARD_ID[0])
                .openFirst();
        assertTableField("Тип сообщения:", "changeCardStatusRequest");
        assertTableField("Запрос:", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> " +
                "<changeCardStatusRequest xmlns=\"http://www.bpc.ru/fmEntityService/\"> <cardId>" + CARD_ID[0] + "</cardId> " +
                "<cardNumber>" + PAN[0] + "</cardNumber>" +
                " <period>0</period> <reason>1</reason> <requestedStatus>1</requestedStatus> <currentStatus>Restricted 167-FZ</currentStatus> " +
                "</changeCardStatusRequest>");
    }

    @Test(
            description = "4. Перейти в карточку клиента КАФ №2 и нажать на кнопку «Разблокировать все карты 167-ФЗ»" +
                    "5. Проверить в логированных сообщениях отправку запроса на разблокировку карт по данному клиенту",
            dependsOnMethods = "clientsCAF1"
    )
    public void clientsCAF2() {
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Клиенты Альфа")
                .setTableFilterWithActive("ИД клиента", "Equals", dboId2)
                .runReport()
                .openFirst()
                .getActionsClientButton167FZ();

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[4]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
        assertEquals(res.getFildsValuesLog(2)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(2)[1], "KAF");

        getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .setTableFilterLog("Идентификатор", "Equals", CARD_ID[4])
                .openFirst();
        assertTableField("Тип сообщения:", "changeCardStatusRequest");
        assertTableField("Запрос:", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> " +
                "<changeCardStatusRequest xmlns=\"http://www.bpc.ru/fmEntityService/\"> <cardId>" + CARD_ID[4] + "</cardId> " +
                "<cardNumber>" + PAN[4] + "</cardNumber>" +
                " <period>0</period> <reason>1</reason> <requestedStatus>1</requestedStatus> <currentStatus>K</currentStatus> " +
                "</changeCardStatusRequest>");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}