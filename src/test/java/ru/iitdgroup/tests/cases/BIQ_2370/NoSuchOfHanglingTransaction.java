package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NoSuchOfHanglingTransaction extends RSHBCaseTest {

    private static final String TABLE = "(Policy_parameters) Параметры обработки событий";
    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 8, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();
    private String IP = "%s.%s.%s.%s";

    @BeforeClass
    public void beforeClass() {
        IP = String.format(IP,
                ThreadLocalRandom.current().nextInt(0, 255) + "",
                ThreadLocalRandom.current().nextInt(0, 255) + "",
                ThreadLocalRandom.current().nextInt(0, 255) + "",
                ThreadLocalRandom.current().nextInt(0, 255) + "");
    }

    @Test(
            description = "Включение правила"

    )
    public void enableRuleForAlert() {
        System.out.println("\"Обработка транзакции без учета справочника\" -- BIQ2370" + " ТК№15");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(5);
    }

    @Test(
            description = "Очистить справочник \"Параметры обработки событий\" от всех записей",
            dependsOnMethods = "enableRuleForAlert"
    )
    public void refactorWorkFlow() {
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "refactorWorkFlow"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакцию, которая не попадет под обработку согласно справочнику из предусловия",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionSERVICE_PAYMENT_1();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().getPC().setIpAddress(IP);
        sendAndAssert(transaction);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Статус АДАК:","DISABLED");
        getIC().close();
        System.out.printf("Тест кейс выполнен успешно!");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionSERVICE_PAYMENT_1() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
