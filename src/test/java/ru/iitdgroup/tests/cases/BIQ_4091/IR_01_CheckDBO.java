package ru.iitdgroup.tests.cases.BIQ_4091;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IR_01_CheckDBO extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_IR_01_CheckDBO";
    private static final String TABLE = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило и добавляем В справочник \"Проверяемые типы транзакций и каналы ДБО\" " +
                    "занести запись для \"Интернет банк\" и \"Транзакция через госуслуги\"",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);

        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(TABLE)
                .addRecord()
                .getElement("Тип транзакции:")
                .getSelect("Запрос в госуслуги")
                .tapToSelect()
                .select("Наименование канала:","Интернет клиент")
                .save();
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "Провести транзакцию № 1 из \"Интернет банк\" типа \"Транзакция через госуслуги\"",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, FALSE_EX_IR1);
    }

    @Test(
            description = "Провести транзакцию № 2 из \"Мобильный банк\" типа \"Транзакция через госуслуги\"",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_FOR_GOSUSLUGI_MOBILE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EX_IR1);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step2"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_FOR_GOSUSLUGI_MOBILE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_FOR_GOSUSLUGI_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
