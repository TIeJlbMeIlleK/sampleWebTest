package ru.iitdgroup.tests.apidriver;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TransactionTest {

    @Test
    public void testToString1() throws IOException, JAXBException {
        final String filled = new Transaction("transactions/tran1.xml")
                .withDBOId("dbId11111111")
                .withTransactionId("TransactionId1231312312313")
                .withCIFId("CIFId5645566357637")
                .toString();

        assertTrue(filled.contains("dbId11111111"));
        assertTrue(filled.contains("TransactionId1231312312313"));
        assertTrue(filled.contains("CIFId5645566357637"));
    }

}