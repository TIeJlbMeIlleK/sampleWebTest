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

    private static final String PHONE1 = "1";
    private static final String PHONE2 = "0";
    private static final String RULE_NAME = "";
    private static final String REFERENCE_ITEM = "";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

//
//    @Test(
//            description = "Настройка и включение правила"
//    )
//    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .selectRule(RULE_NAME)
//                .activate()
//                .sleep(15);
//    }

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
        transactionData
                .getServicePayment()
                .withAdditionalField(getPhoneField(PHONE1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(PHONE1, "vetka");
    }

    @Test(
            description = "Отправить транзакции №1" +
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
        transactionData
                .getServicePayment()
                .withAdditionalField(getPhoneField(PHONE2));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(PHONE2,"vetka");
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private AdditionalFieldType getPhoneField(String phone) {
        return new AdditionalFieldType()
                .withId("account")
                .withName("vetka")
                .withValue(phone);
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
