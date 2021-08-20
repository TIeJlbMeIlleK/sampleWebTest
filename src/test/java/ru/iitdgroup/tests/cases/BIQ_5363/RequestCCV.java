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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RequestCCV extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String RULE_NAME_1 = "R01_GR_20_NewPayee";
    private static final String RULE_NAME_2 = "R01_WR_02_BudgetTransfer";
    private static final String RULE_NAME_3 = "R01_IR_03_RepeatApprovedTransaction";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};

    @Test(
            description = "Включаем правило"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .setFilterAndSelectRule("Name", "Equals", RULE_NAME)
                .activate()
                .setFilterAndSelectRule("Name", "Equals", RULE_NAME_1)
                .activate()
                .setFilterAndSelectRule("Name", "Equals", RULE_NAME_2)
                .activate()
                .setFilterAndSelectRule("Name", "Equals", RULE_NAME_3)
                .activate()
                .sleep(20);
        getIC().locateRules();

        commandServiceMock.run();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
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
            description = "-- «Запрос CVC/CVV/CVP»,Интернет банк, устройство ПК" +
                    "-- «Запрос CVC/CVV/CVP»,Мобильный банк, устройство IOC" +
                    "-- «Запрос CVC/CVV/CVP»,Мобильный банк, устройство Android",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransactionREQUEST_CCV();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        String tran = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Transaction transaction1 = getTransactionREQUEST_CCV_Android();
        TransactionDataType transactionData1 = transaction1.getData().getTransactionData();
        String tran1 = transactionData1.getTransactionId();
        sendAndAssert(transaction1);

        Transaction transaction2 = getTransactionREQUEST_CCV_IOC();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData();
        String tran2 = transactionData2.getTransactionId();
        sendAndAssert(transaction2);

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Нет совпадений по параметрам со списками запрещенных");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran1)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_1)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применяется для транзакций такого типа");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_2)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применилось");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_3)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Непроверяемый тип транзакции");

    }

    @Test(
            description = "-- «Запрос реквизитов карты»,Интернет банк, устройство ПК" +
                    "-- «Запрос реквизитов карты»,Мобильный банк, устройство IOC" +
                    "-- «Запрос реквизитов карты»,Мобильный банк, устройство Android",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_PAN();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        String tran = transactionData.getTransactionId();
        sendAndAssert(transaction);

        Transaction transaction1 = getTransactionREQUEST_PAN_Android();
        TransactionDataType transactionData1 = transaction1.getData().getTransactionData();
        String tran1 = transactionData1.getTransactionId();
        sendAndAssert(transaction1);

        Transaction transaction2 = getTransactionREQUEST_PAN_IOC();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData();
        String tran2 = transactionData2.getTransactionId();
        sendAndAssert(transaction2);

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Нет совпадений по параметрам со списками запрещенных");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran1)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_1)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применяется для транзакций такого типа");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran2)
                .setNewTableFilterForTransactions("Название правила","Equals",RULE_NAME_2)
                .runReport().openFirst();
        assertTableField("Результат выполнения:","NOT_TRIGGERED");
        assertTableField("Описание:","Правило не применилось");

        getIC()
                .locateReports()
                .openFolder("Отчеты по правилам")
                .openRecord("Срабатывания правила")
                .setTableFilterForTransactions("Идентификатор трвнзакции ДБО","Equals",tran2)
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
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CCV_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionREQUEST_CCV_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CCV_IOC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_PC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN_Android() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionREQUEST_PAN_IOC() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_PAN_IOC.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }
}
