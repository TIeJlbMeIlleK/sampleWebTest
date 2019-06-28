package ru.iitdgroup.tests.apidriver;

import org.junit.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TransactionTest {

    @Test
    public void testToString1() throws IOException, JAXBException {
        Transaction transaction = new Transaction("transactions/tran1.xml");
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId("dbId11111111")
                .withCifId("CIFId5645566357637");
        transactionData.withTransactionId("TransactionId1231312312313");

        final String filled = transaction.toString();

        assertTrue(filled.contains("dbId11111111"));
        assertTrue(filled.contains("TransactionId1231312312313"));
        assertTrue(filled.contains("CIFId5645566357637"));
    }

}