package ru.iitdgroup.tests.cases.BIQ_5363;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class RequestCCV extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String RULE_NAME_1 = "R01_GR_20_NewPayee";
    private static final String RULE_NAME_2 = "R01_WR_02_BudgetTransfer";
    private static final String RULE_NAME_3 = "R01_IR_03_RepeatApprovedTransaction";
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
            description = "Включаем правило",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
                .save()
                .sleep(2);
        getIC().locateRules()
                .editRule(RULE_NAME_1)
                .fillCheckBox("Active:",true)
                .save()
                .sleep(2);
        getIC().locateRules()
                .editRule(RULE_NAME_2)
                .fillCheckBox("Active:",true)
                .save()
                .sleep(2);
        getIC().locateRules()
                .editRule(RULE_NAME_3)
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);
        commandServiceMock.run();
    }

    @Test(
            description = "-- «Запрос CVC/CVV/CVP»,Интернет банк, устройство ПК\n" +
                    "-- «Запрос CVC/CVV/CVP»,Мобильный банк, устройство IOC\n" +
                    "-- «Запрос CVC/CVV/CVP»,Мобильный банк, устройство Android",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionREQUEST_CCV();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Transaction transaction1 = getTransactionREQUEST_CCV_Android();
        TransactionDataType transactionData1 = transaction1.getData().getTransactionData()
                .withRegular(false);
        transactionData1
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran1 = transactionData1.getTransactionId();
        sendAndAssert(transaction1);

        Transaction transaction2 = getTransactionREQUEST_CCV_IOC();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran2 = transactionData2.getTransactionId();
        sendAndAssert(transaction2);

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Нет совпадений по параметрам со списками запрещенных");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran1)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_1)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применяется для транзакций такого типа");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_2)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применилось");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_3)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Непроверяемый тип транзакции");

    }

    @Test(
            description = "-- «Запрос реквизитов карты»,Интернет банк, устройство ПК\n" +
                    "-- «Запрос реквизитов карты»,Мобильный банк, устройство IOC\n" +
                    "-- «Запрос реквизитов карты»,Мобильный банк, устройство Android",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_PAN();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Transaction transaction1 = getTransactionREQUEST_PAN_Android();
        TransactionDataType transactionData1 = transaction1.getData().getTransactionData()
                .withRegular(false);
        transactionData1
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran1 = transactionData1.getTransactionId();
        sendAndAssert(transaction1);

        Transaction transaction2 = getTransactionREQUEST_PAN_IOC();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        String tran2 = transactionData2.getTransactionId();
        sendAndAssert(transaction2);

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Нет совпадений по параметрам со списками запрещенных");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran1)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_1)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применяется для транзакций такого типа");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_2)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применилось");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Отчет срабатывания правил")
                .setTableFilterForTransactions("ID транзакции","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_3)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Непроверяемый тип транзакции");
        getIC().close();
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

    private Transaction getTransactionREQUEST_CCV() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CCV_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CCV_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_IOC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }


}
