package ru.iitdgroup.tests.cases.BIQ_6250;

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


public class IR_03_RepeatApprovedTransaction_PhoneNT_InitialSourceAmount extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_IR_03_RepeatApprovedTransaction";
    private static final String PAYEE_1 = "79599958564";
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
            description = "Включаем правило и выполняем преднастройки",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Длина серии:","3")
                .fillInputText("Период серии в минутах:","10")
                .fillCheckBox("РДАК выполнен:",false)
                .fillCheckBox("АДАК выполнен:",false)
                .fillCheckBox("Требовать совпадения остатка на счете:",true)
                .select("Тип транзакции:","PHONE_NUMBER_TRANSFER")
                .fillCheckBox("Active:",true)
                .save()
                .sleep(30);
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = " Отправить Транзакцию №1 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_1);
        transactionData
                .getPhoneNumberTransfer()
                .setBankName("РОССЕЛЬХОЗБАНК");
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, PHONE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №2 в обработку -- Получатель №1, сумма 500, остаток 9500",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_1);
        transactionData
                .getPhoneNumberTransfer()
                .setBankName("РОССЕЛЬХОЗБАНК");
        transactionData
                .getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(9500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, PHONE_CONDITIONS_NOT_MET);
    }

    @Test(
            description = "Отправить Транзакцию №3 в обработку -- Получатель №1, сумма 500, остаток 10000",
            dependsOnMethods = "step2"
    )

    public void step3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(PAYEE_1);
        transactionData
                .getPhoneNumberTransfer()
                .setBankName("РОССЕЛЬХОЗБАНК");
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000));
        transactionData
                .getPhoneNumberTransfer()
                .setAmountInSourceCurrency(BigDecimal.valueOf(500));
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, TRIGGERED_TRUE);
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

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
