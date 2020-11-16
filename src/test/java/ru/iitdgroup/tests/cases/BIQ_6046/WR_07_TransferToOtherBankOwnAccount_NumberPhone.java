package ru.iitdgroup.tests.cases.BIQ_6046;

import com.google.i18n.phonenumbers.Phonenumber;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.ContactChannelType;
import ru.iitdgroup.intellinx.dbo.client.ContactInfoType;
import ru.iitdgroup.intellinx.dbo.client.ContactKind;
import ru.iitdgroup.intellinx.dbo.client.ContactType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class WR_07_TransferToOtherBankOwnAccount_NumberPhone extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_WR_07_TransferToOtherBankOwnAccount";
    //private static final String REFERENCE_ITEM = "(Rule_tables) Запрещенные получатели БИКСЧЕТ";
    private static final String phoneNumberAuth = "79101111111";
    private static final String phoneNumberNotis = "79102222222";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(15);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void step0() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
                        .withFirstName("Иван")
                        .withLastName("Иванов")
                        .withMiddleName("Иванович")
                        .getClientIds()
                        .withDboId(dboId);

                for (ContactType c : client.getData().getClientData().getContactInfo().getContact()) {
                    if (c.getContactKind().value().equals("AUTH")) {
                        c.setValue(phoneNumberAuth);
                    }
                    else if (c.getContactKind().value().equals("NOTIFICATION")) {
                        c.setValue(phoneNumberNotis);
                    }
                }

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакцию №1 Перевод по номеру телефона с  PayeeName = \"Иванов Иван И\" и PayeePhone = Номер телефона №1",
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
                .setPayeeName("Иванов Иван И");
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(phoneNumberAuth);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES);
    }

    @Test(
            description = "Отправить транзакцию №2 Перевод по номеру телефона с  PayeeName = \"Иван Иванов Ф\" и PayeePhone = Номер телефона №1",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeeName("Иван Иванов Ф");
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(phoneNumberAuth);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES);
    }

    @Test(
            description = "Отправить транзакцию №3 Перевод по номеру телефона с  PayeeName = \"Иван Иванов И\" и PayeePhone = Номер телефона №2",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeeName("Иван Иванов И");
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(phoneNumberNotis);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES);
    }

    @Test(
            description = "Отправить транзакцию №4 Перевод по номеру телефона с  PayeeName = \"Иван Иванович И\" и PayeePhone = Номер телефона №1",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeeName("Иван Иванович И");
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone(phoneNumberAuth);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EXISTS_MATCHES);
    }

    @Test(
            description = "Отправить транзакцию №5 Перевод по номеру телефона с PayeeName = \"Иван Иванович И\" и незаполненным полем PayeePhone",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPhoneNumberTransfer()
                .setPayeeName("Иван Иванович И");
        transactionData
                .getPhoneNumberTransfer()
                .setPayeePhone("");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось, т.к. отсутствует номер телефона получателя");
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
