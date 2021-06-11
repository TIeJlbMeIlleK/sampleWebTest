package ru.iitdgroup.tests.cases.BIQ_7993;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.rabbit.Rabbit;
import ru.iitdgroup.tests.webdriver.report.ReportRecord;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.assertEquals;

public class Restricted167FZ_DBO extends RSHBCaseTest {
    private static final String RULE_NAME = "R01_GR_20_NewPayee";

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Юрий", "Глызин", "Андреевич"}, {"Олег", "Тырин", "Семенович"}, {"Максим", "Туров", "Олегович"}};

    private static final String CARD_HOLDER_NAME = "Глызин Юрий Андреевич";
    private static final String CARD_HOLDER_NAME2 = "Тырин Олег Семенович";

    private static final String ACCOUNT = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);
    private static final String ACCOUNT2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

    private static final String PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String PHONE2 = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);

    private final static String[] PAN = new String[7];
    private static final String[] CARD_ID = {
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
            (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4),
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

    //TODO перед запуском должен быть создан Action для Клиент ДБО WF:
    // -- Display name: Разблокировать все карты 167-ФЗ
    // -- Unique name: massunblock
    // -- В ExternalApi выбрать "(CCAF) Send Card command"
    // Статусная доступность из Any State в Keep Current State


    @Test(
            description = "Создание клиентов"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 3; i++) {
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
    }

    @Test(
            description = "Создаем клиентов КАФ",
            dependsOnMethods = "addClients"
    )
    public void addClientsCAF() {
        try {
            for (int i = 0; i < 7; i++) {
                PAN[i] = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 13);

                String cafClientResponse = getRabbit()// создание клиента КАФ с разными статусами карт
                        .getAllQueues()
                        .getQueue(getProps().getRabbitCafClientQueueName())
                        .getCafClientResponse();
                JSONObject json = new JSONObject(cafClientResponse);

                json.put("cardId", CARD_ID[i]);
                json.put("pan", PAN[i]);

                if (i <= 4) {
                    json.put("clientId", clientIds.get(0));
                    json.put("cardholderName", CARD_HOLDER_NAME);
                    json.put("account", ACCOUNT);
                    json.put("3DsecPhone", PHONE);
                    json.put("cardStatus", CARD_STATUS[i]);
                    System.out.println("ID клиента № 1= " + clientIds.get(0));
                    System.out.println("Аккаунт клиента № 1 = " + ACCOUNT);
                    System.out.println("Статус карты №" + i + " = " + CARD_STATUS[i]);
                } else if (i == 5) {
                    json.put("clientId", clientIds.get(1));
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[1]);
                    System.out.println("ID клиента № 2 = " + clientIds.get(2));
                    System.out.println("Аккаунт клиента № 2 = " + ACCOUNT2);
                    System.out.println("Статус карты №" + i + " = " + CARD_STATUS[1]);
                } else {
                    json.put("clientId", clientIds.get(1));
                    json.put("cardholderName", CARD_HOLDER_NAME2);
                    json.put("account", ACCOUNT2);
                    json.put("3DsecPhone", PHONE2);
                    json.put("cardStatus", CARD_STATUS[5]);
                    System.out.println("ID клиента № 2 = " + clientIds.get(2));
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
            description = "1. Перейти в карточку клиента ДБО №1 и выполнить Action Разблокировать все карты 167-ФЗ" +
                    "2. Проверить в логированных сообщениях отправку запроса на разблокировку карт по данному клиенту" +
                    "-- Проверить корректность формирования полей в запросе",
            dependsOnMethods = "addClientsCAF"
    )
    public void clientsDBO1() {

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("Разблокировать все карты 167-ФЗ")
                .approved();

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
    }

    @Test(
            description = "4. Перейти в карточку клиента КАФ №2 и выполнить Action Разблокировать все карты 167-ФЗ" +
                    "5. Проверить в логированных сообщениях отправку запроса на разблокировку карт по данному клиенту",
            dependsOnMethods = "clientsDBO1"
    )
    public void clientsDBO2() {
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(1))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("Разблокировать все карты 167-ФЗ")
                .approved();

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[6]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
        assertEquals(res.getFildsValuesLog(2)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(2)[1], "KAF");
    }

    @Test(
            description = "7. Перейти в карточку клиента ДБО №3 и выполнить Action из предусловия" +
                    "8. Проверить в логированных сообщениях отправку запроса на разблокировку карт по данному клиенту" +
                    "-- Проверить корректность формирования полей в запросе",
            dependsOnMethods = "clientsDBO2"
    )
    public void clientsDBO3() {
        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(2))
                .runReport()
                .openFirst()
                .getActionsForClient()
                .doAction("Разблокировать все карты 167-ФЗ")
                .approved();

        ReportRecord res = getIC().locateReports()
                .openFolder("Системные отчеты")
                .openRecord("Логированные сообщения")
                .removeAllFilters()
                .runReport();
        assertEquals(res.getFildsValuesLog(1)[3], CARD_ID[6]);
        assertEquals(res.getFildsValuesLog(1)[1], "KAF");
        assertEquals(res.getFildsValuesLog(2)[3], CARD_ID[0]);
        assertEquals(res.getFildsValuesLog(2)[1], "KAF");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}