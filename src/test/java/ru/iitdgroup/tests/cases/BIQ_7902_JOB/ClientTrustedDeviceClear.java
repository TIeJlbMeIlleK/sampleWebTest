package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientTrustedDeviceClear extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private final GregorianCalendar time1 = new GregorianCalendar();
    private final GregorianCalendar time2 = new GregorianCalendar();

    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}};
    private static String RULE_NAME;
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String TRUSTED_DEVICES = "(Rule_tables) Доверенные устройства для клиента";
    private static final String IMEI1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String IMEI2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String IMSI1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String IMSI2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);

    @Test(
            description = "Создаем клиента"
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
            description = "1. Занести в \"Доверенные устройства\" Устройство № 1 с \"Дата занесения\" более 1 дня назад" +
                    "2. Занести в \"Доверенные устройства\" Устройство № 2 с \"Дата занесения\" менее 1 дня назад" +
                    " REMOVAL_DATE = 1 (справочник \"Параметры обработки справочников и флагов\")",
            dependsOnMethods = "addClients"
    )
    public void tableEdit() {
        time1.add(Calendar.HOUR, -28);
        time2.add(Calendar.HOUR, -15);
        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "REMOVAL_DATE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
        getIC().locateTable(TRUSTED_DEVICES)
                .deleteAll()
                .addRecord()
                .fillCheckBox("Доверенный:", true)
                .fillInputText("IMEI:", IMEI1)
                .fillInputText("IMSI:", IMSI1)
                .fillInputText("Дата занесения:", format.format(time1.getTime()))
                .fillUser("Клиент:", clientIds.get(0))
                .save();
        getIC().locateTable(TRUSTED_DEVICES)
                .addRecord()
                .fillCheckBox("Доверенный:", true)
                .fillInputText("IMEI:", IMEI2)
                .fillInputText("IMSI:", IMSI2)
                .fillInputText("Дата занесения:", format.format(time2.getTime()))
                .fillUser("Клиент:", clientIds.get(0))
                .save();
    }

    @Test(
            description = "Запустить джоб ClientTrustedDeviceClear и проверить наличие доверенных устройств у клиента",
            dependsOnMethods = "tableEdit"
    )
    public void runJobStep2() {
        getIC().locateJobs()
                .selectJob("ClientTrustedDeviceClear")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(TRUSTED_DEVICES)//проверка наличия записи в доверенных
                .refreshTable()
                .findRowsBy()
                .match("IMEI", IMEI2)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(TRUSTED_DEVICES)//проверка удаления записи из доверенных после отработки JOB
                .refreshTable()
                .findRowsBy()
                .match("IMEI", IMEI1)
                .failIfRowsExists();//проверка справочника на отсутствие записи
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }
}
