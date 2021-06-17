package ru.iitdgroup.tests.cases.BIQ_5377;

import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JOB_PayeeWhiteListClear extends RSHBCaseTest {

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Инна", "Пашкина", "Марковна"}, {"Николай", "Хрюков", "Михайлович"}};
    private static final String RULE_NAME = "";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";
    private static final String TYPE_TSP1 = new RandomString(8).nextString();
    private static final String TYPE_TSP2 = new RandomString(8).nextString();

    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 2; i++) {
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
            description = "Занести в справочник Доверенные получатели запись № 1 для клиент-получатель №1, " +
                    "где Дата занесения более 1 дня назад" +
                    "Занести в справочник Доверенные получатели запись № 2 для клиент-получатель №2," +
                    " где Дата занесения менее 1 дня назад" +
                    "RemovalDate = 1 (справочник Параметры обработки справочников и флагов)",
            dependsOnMethods = "addClient"
    )
    public void makeChangesToTheDirectory() {
        getIC().locateTable(REFERENCE_ITEM2)
                .deleteAll();

        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", TYPE_TSP1)
                .save();
        //меняем в БД дату занесения и дату последней транзакции у 1го клиента более 1 дня назад(-28 часов)
        Instant transactionTime1 = Instant.now().minus(28, ChronoUnit.HOURS);

        HashMap<String, Object> map = new HashMap<>();
        map.put("TIME_STAMP", transactionTime1.toString());
        map.put("LAST_TRANSACTION", transactionTime1.toString());
        getDatabase().updateWhere("WHITE_LIST", map, "WHERE [id] = (SELECT MAX([id]) FROM [WHITE_LIST])");


        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .fillInputText("Имя получателя:", TYPE_TSP2)
                .save().sleep(3);
        //меняем в БД дату занесения и дату последней транзакции у 2го клиента менее 1 дня назад(-15 часов)
        Instant transactionTime2 = Instant.now().minus(15, ChronoUnit.HOURS);

        map = new HashMap<>();
        map.put("TIME_STAMP", transactionTime2.toString());
        map.put("LAST_TRANSACTION", transactionTime2.toString());
        getDatabase().updateWhere("WHITE_LIST", map, "WHERE [id] = (SELECT MAX([id]) FROM [WHITE_LIST])");

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy().match("код значения", "REMOVAL_DATE").click()
                .edit().fillInputText("Значение:", "1").save().sleep(1);
    }

    @Test(
            description = "Запустить джоб PayeeWhiteListClear",
            dependsOnMethods = "makeChangesToTheDirectory"
    )

    public void runJobStep() {

        getIC().locateJobs()
                .selectJob("PayeeWhiteListClear")
                .run()
                .waitStatus(JobRunEdit.JobStatus.SUCCESS);
        getIC().home();
    }

    @Test(
            description = "Проверить справочник \"Доверенные получатели\"",
            dependsOnMethods = "runJobStep"
    )

    public void checkingReferenceBooks() {
        String name1 = names[0][1] + ' ' + names[0][0] + ' ' + names[0][2];
        String name2 = names[1][1] + ' ' + names[1][0] + ' ' + names[1][2];

        getIC().locateTable(REFERENCE_ITEM2)//проверка наличия записи в доверенных
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP2)
                .match("ФИО Клиента", name2)
                .failIfNoRows(); //проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM2)//проверка удаления записи из доверенных после отработки JOB
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP1)
                .match("ФИО Клиента", name1)
                .failIfRowsExists(); //проверка справочника на отсутствие записи
        getIC().close();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}
