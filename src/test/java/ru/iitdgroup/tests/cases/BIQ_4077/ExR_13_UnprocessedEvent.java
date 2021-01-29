package ru.iitdgroup.tests.cases.BIQ_4077;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.IOSDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Authentication;
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

public class ExR_13_UnprocessedEvent extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_13_UnprocessedEvent";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final List<String> clientIds = new ArrayList<>();
    private final GregorianCalendar time = new GregorianCalendar(2021, Calendar.JANUARY, 7, 0, 0, 0);
    private final GregorianCalendar time1 = new GregorianCalendar(2021, Calendar.JANUARY, 7, 0, 0, 0);
    private final GregorianCalendar time2 = new GregorianCalendar(2021, Calendar.JANUARY, 7, 0, 0, 0);




    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Время в минутах:","60")
                .fillCheckBox("Active:",true)
                .save()
                .getDriver()
                .findElementByXPath("//*[@id=\"j_id107:0:breadcrumb\"]")
                .click();


        getIC().locateRules()
                .selectVisible()
                .editRule("R01_GR_120_ESPP_NewRecipient")
                .fillCheckBox("Active:",true)
                .fillInputText("Задержка:","3")
                .save()
                .getDriver()
                .findElementByXPath("//*[@id=\"j_id107:0:breadcrumb\"]")
                .click();

        getIC().locateRules()
                .selectVisible()
                .editRule("R01_GR_15_NonTypicalGeoPosition")
                .fillCheckBox("Active:", true)
                .save()
                .sleep(25);

        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Код значения", "Integr_ESPP_CROSSCHANNEL")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }


    @Test(
            description = "Отправить транзакцию ЕСПП от Клиента №1 для формирования Алерта",
            dependsOnMethods = "client"
    )
    public void sendTransESPP() {
        //TODO требуется реализовать отправку сообщения от ЕСПП
        commandServiceMock.run();
//        Transaction transaction = getTransactionESPP();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//        .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
//                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2));
//        transactionData
//                .getClientIds()
//                .withEksId(clientIds.get(0));
//        sendAndAssert(transaction);
//        assertLastTransactionRuleApply(TRIGGERED, SUSPICIOUS_DEVICE);
    }

    @Test(
            description = "Отправить транзакцию ДБО от Клиента №2 для формирования Алерта",
            dependsOnMethods = "sendTransESPP"
    )
    public void sendTransDBO() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
    }

    @Test(
            description = "От клиента №1 отправить транзакцию №1 типа \"Запрос на выдачу кредита\", спустя 5 минут после формирования события ЕСПП",
            dependsOnMethods = "sendTransDBO"
    )
    public void sendTrans_1() {
        time.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY);

    }

    @Test(
            description = "От клиента №1 отправить транзакцию №2 типа \"Запрос на выдачу кредита\", спустя 61 минуту после формирования события ЕСПП",
            dependsOnMethods = "sendTrans_1"
    )
    public void sendTrans_2() {
        time.add(Calendar.MINUTE, 61);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "От клиента №2 отправить транзакцию №3 типа \"Запрос на выдачу кредита\", спустя 5 минут после формирования события ДБО",
            dependsOnMethods = "sendTrans_2"
    )
    public void sendTrans_3() {
        time1.add(Calendar.MINUTE, 5);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_APPLY);
    }

    @Test(
            description = "От клиента №2 отправить транзакцию №4 типа \"Запрос на выдачу кредита\", спустя 61 минуту после формирования события ДБО",
            dependsOnMethods = "sendTrans_3"
    )
    public void sendTrans_4() {
        time1.add(Calendar.MINUTE, 61);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1))
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "От клиента №2 отправить транзакцию №4 типа \"Запрос на выдачу кредита\", спустя 61 минуту после формирования события ДБО",
            dependsOnMethods = "sendTrans_3"
    )
    public void sendTrans_5() {
        time2.add(Calendar.MINUTE, 6);
        Transaction transaction = getTransactionGETTING_CREDIT_Android();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time2))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time2))
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getGettingCredit()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        transactionData.getClientDevice().getAndroid().setIpAddress("178.219.186.12");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, REGULAR_TRANSACTION_EXR9);
        commandServiceMock.stop();
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT_Android() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_Android.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionESPP() {
        Transaction transaction = getTransactionESPP("testCases/Templates/ESPP_transaction.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
