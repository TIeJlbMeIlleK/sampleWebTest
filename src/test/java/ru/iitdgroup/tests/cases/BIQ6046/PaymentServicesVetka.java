package ru.iitdgroup.tests.cases.BIQ6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.AdditionalFieldType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import javax.xml.soap.Name;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class PaymentServicesVetka extends RSHBCaseTest {

    private static final String RULE_NAME = "";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Создаем клиента"
    )
    public void step0() {
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
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакции №1" +
                    "-- «Parameter.Name» =  vetka" +
                    "-- «Parameter.Value» = 1",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        updateTransactionAdditionalFields(transactionData, "ACCOUNT", "vetka", "1");

        sendAndAssert(transaction);
        assertTransactionAdditionalFieldApply(transactionData.getTransactionId(), "ACCOUNT", "vetka", "1");
    }


    @Test(
            description = "Отправить транзакции №2" +
                    "-- «Parameter.Name» =  vetka" +
                    "-- «Parameter.Value» = 0",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        updateTransactionAdditionalFields(transactionData, "ACCOUNT", "vetka", "0");

        sendAndAssert(transaction);
        assertTransactionAdditionalFieldApply(transactionData.getTransactionId(), "ACCOUNT", "vetka", "0");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private AdditionalFieldType getPhoneField(String phone) {
        return new AdditionalFieldType()
                .withId("ACCOUNT")
                .withName("vetka")
                .withValue(phone);
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_MB.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private void updateTransactionAdditionalFields(TransactionDataType transactionData, String id, String nameForNewField, String value) {
        List<AdditionalFieldType> l = transactionData
                .getServicePayment()
                .getAdditionalField();
        boolean exists = false;
        for (AdditionalFieldType f : l) {
            if (f.getId().equals(id)) {
                f.setValue(value);
                exists = true;
            }
        }
        for (AdditionalFieldType k : l) {
            if (k.getId().equals(id)) {
                k.setName(nameForNewField);
                exists = true;
            }
        }

        for (AdditionalFieldType j : l) {
            if (j.getId().equals(id)) {
                j.setId(id);
                exists = true;
            }
        }
        if (!exists) {
            l.add(new AdditionalFieldType()
                    .withId(id)
                    .withName(nameForNewField)
                    .withValue(value)
            );
        }

    }
}
