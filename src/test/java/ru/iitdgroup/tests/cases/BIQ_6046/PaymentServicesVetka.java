package ru.iitdgroup.tests.cases.BIQ_6046;

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

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Петр", "Урин", "Семенович"}};

    @Test(
            description = "Создаем клиента"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
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
            description = "Отправить транзакции №1" +
                    "-- «Parameter.Name» =  vetka" +
                    "-- «Parameter.Value» = 1",
            dependsOnMethods = "addClients"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        updateTransactionAdditionalFields(transactionData, "ACCOUNT", "vetka", "1");
        transactionData
                .getServicePayment()
                .getAdditionalField();
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
        transactionData
                .getServicePayment()
                .getAdditionalField();
        sendAndAssert(transaction);
        assertTransactionAdditionalFieldApply(transactionData.getTransactionId(), "ACCOUNT", "vetka", "0");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
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
                f.setName(nameForNewField);
                exists = true;
                break;
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
