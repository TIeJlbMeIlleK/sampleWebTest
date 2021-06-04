package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RemoveTypicalLocationJob extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}};
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String REFERENCE_ITEM_TUPICAL_LOCATION = "(Rule_tables) Типичное расположение";

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
    }

    @Test(
            description = "Заполнить справочники",
            dependsOnMethods = "addClients"
    )
    public void tableEdit() {
        time1.add(Calendar.HOUR, -32);
        time2.add(Calendar.HOUR, -15);
        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "REMOVAL_DATE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)
                .deleteAll()
                .addRecord()
                .fillInputText("Кол-во посещений:", "1")
                .fillInputText("Долгота:", "37,62")
                .fillInputText("Широта:", "55,75")
                .fillInputText("Страна:", "Россия")
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Дата последней транзакции:", format.format(time1.getTime()))
                .save();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)
                .addRecord()
                .fillInputText("Кол-во посещений:", "1")
                .fillInputText("Долгота:", "91,42")
                .fillInputText("Широта:", "53,71")
                .fillInputText("Страна:", "Россия")
                .fillUser("Клиент:", clientIds.get(0))
                .fillInputText("Дата последней транзакции:", format.format(time2.getTime()))
                .save();
    }

    @Test(
            description = "Запустить джоб RemoveTypicalLocationJob и проверить Типичное расположени",
            dependsOnMethods = "tableEdit"
    )
    public void runJob() {

        getIC().locateJobs()
                .selectJob("RemoveTypicalLocationJob")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка наличия записи в типичном
                .refreshTable()
                .findRowsBy()
                .match("Долгота", "91.42")
                .match("Широта", "53.71")
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(REFERENCE_ITEM_TUPICAL_LOCATION)//проверка удаления записи из типичного после отработки JOB
                .refreshTable()
                .findRowsBy()
                .match("Долгота", "37.62")
                .match("Широта", "55.75")
                .failIfRowsExists();//проверка справочника на отсутствие записи
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}
