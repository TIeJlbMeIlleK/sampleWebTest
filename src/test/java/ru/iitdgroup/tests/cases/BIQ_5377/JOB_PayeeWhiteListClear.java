package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class JOB_PayeeWhiteListClear extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private GregorianCalendar time2;
    private GregorianCalendar time3;
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final DateFormat format2 = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Яна", "Янова", "Марковна"}, {"Соня", "Хрюкова", "Михайловна"}};


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
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Занести в справочник \"Доверенные получатели\" запись № 1 для клиент-получатель №1, " +
                    "где \"Дата занесения\" более 1 дня назад" +
                    "Занести в справочник \"Доверенные получатели\" запись № 2 для клиент-получатель №2," +
                    " где \"Дата занесения\" менее 1 дня назад" +
                    "RemovalDate = 1 (справочник \"Параметры обработки справочников и флагов\")",
            dependsOnMethods = "addClient"
    )
    public void makeChangesToTheDirectory() {
        Table.Formula rows1 = getIC().locateTable(REFERENCE_ITEM2).findRowsBy();

        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {//очищает доверенных
            rows1.selectLinesAndDelete();
        }
        time.add(Calendar.HOUR, -28);
        time2 = (GregorianCalendar) time.clone();
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", TYPE_TSP1)
                .fillInputText("Дата занесения:", format2.format(time2.getTime()))
                .save();

        time.add(Calendar.HOUR, -15);
        time3 = (GregorianCalendar) time.clone();
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(1))
                .fillInputText("Имя получателя:", TYPE_TSP2)
                .fillInputText("Дата занесения:", format2.format(time3.getTime()))
                .save();

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
        String name1 = names[0][0] + ' ' + names[0][1] + ' ' + names[0][2];
        String name2 = names[1][0] + ' ' + names[1][1] + ' ' + names[1][2];

        getIC().locateTable(REFERENCE_ITEM2)//проверка удаления записи из доверенных после отработки JOB
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP1)
                .match("Дата занесения", format.format(time2.getTime()))
                .match("ФИО Клиента", name1)
                .failIfRowsExists(); //проверка справочника на отсутствие записи


        getIC().locateTable(REFERENCE_ITEM2)//проверка наличия записи в доверенных
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", TYPE_TSP2)
                .match("Дата занесения", format.format(time3.getTime()))
                .match("ФИО Клиента", name2)
                .failIfNoRows(); //проверка справочника на наличие записи

        getIC().close();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
