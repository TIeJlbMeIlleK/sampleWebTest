package ru.iitdgroup.tests.cases.BIQ_4077;

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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Thread.sleep;


public class ExR_12_HackADAK extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_ExR_12_HackADAK";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    int year = Calendar.getInstance().get(Calendar.YEAR);
    int month = Calendar.getInstance().get(Calendar.MONTH);
    int dayOfMonth = Calendar.getInstance().get(Calendar.DATE);
    int hourOfDate = Calendar.getInstance().get(Calendar.HOUR);
    int minuteOfHour = Calendar.getInstance().get(Calendar.MINUTE);
    private final GregorianCalendar time = new GregorianCalendar(year, month, dayOfMonth, hourOfDate, minuteOfHour, 0);
    private final List<String> clientIds = new ArrayList<>();
    public String tranID;

    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 3; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
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
            description = "Включаем правило и взводим флаги по клиентам",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:",true)
                .fillInputText("Статусы АДАК:","WRONG, REFUSE")
                .fillInputText("Период серии (в минутах):","10")
                .fillInputText("Количество неуспешных попыток :","1")
                .save()
                .getDriver()
                .findElementByXPath("//*[@id=\"j_id107:0:breadcrumb\"]")
                                .click();


        getIC().locateRules()
                .selectVisible()
                .editRule("R01_ExR_05_GrayIP")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);

        Table.Formula rows = getIC().locateTable("(Rule_tables) Подозрительные IP адреса").findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable("(Rule_tables) Подозрительные IP адреса")
                .addRecord()
                .fillInputText("Маска подсети устройства:","10.152.150.0/24")
                .fillInputText("IP устройства:","10.152.150.1")
                .save();

        Table.Formula rows1 = getIC().locateTable("(Policy_parameters) Параметры обработки событий").findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable("(Policy_parameters) Параметры обработки событий")
                .addRecord()
                .getElement("Наименование группы клиентов:")
                .refresh()
                .getSelect("Группа по умолчанию")
                .tapToSelect()
                .fillCheckBox("Требуется выполнение АДАК:",true)
                .fillCheckBox("Требуется выполнение РДАК:",true)
                .select("Наименование канала ДБО:","Интернет клиент")
                .getElement("Тип транзакции:")
                .getSelect("Запрос на выдачу кредита")
                .tapToSelect()
                .save();
        commandServiceMock.run();
    }

    @Test(
            description = "Отправить транзакцию №1 и ответить не верно",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.152.150.1");
        tranID = transactionData.getTransactionId();
        sendAndAssert(transaction);

        try {
            Thread.sleep(12_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getIC().locateAlerts()
                .openFirst()
                .sleep(5)
                .action("Выполнить АДАК");
        getIC().close();
    }

    @Test(
            description = "Отправить АДАК с неверным ответом",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction adak = getTransactionAdak();
        TransactionDataType adakData = adak.getData().getTransactionData()
                .withRegular(false);
        adakData
                .getClientIds()
                .withDboId(clientIds.get(0));
        adakData
                .getClientIds()
                .withExpertSystemId(clientIds.get(0));
        adakData
                .getClientIds()
                .setCifId(clientIds.get(0));
        adakData
                .withTransactionId(tranID);
        adakData
                .getAdditionalAnswer()
                .setAdditionalAuthAnswer("123456");
        sendAndAssert(adak);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Отправить транзакцию №2",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("10.152.150.1");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY_ADAK);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step3"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionAdak() {
        Transaction transaction = getTransaction("testCases/Templates/ADAK.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
