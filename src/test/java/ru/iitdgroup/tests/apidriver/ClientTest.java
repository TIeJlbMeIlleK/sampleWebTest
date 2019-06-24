package ru.iitdgroup.tests.apidriver;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.*;

public class ClientTest extends ApiDriverTest {

    private Client client;

    @Before
    public void before() throws JAXBException, IOException {
        client = new Client("clients/client1.xml");
    }

    @Test
    public void testParse() {
        assertNotNull(client.getData());
        assertEquals(client.getData().getClientData().getClient().getFirstName(), "Mikhail");
        assertEquals(client.getData().getClientData().getClient().getLastName(), "Makarov");
        assertEquals(client.getData().getClientData().getClient().getMiddleName(), "Sergeevich");
    }

    @Test
    public void testSetFirstName() {
        client.getData().getClientData().getClient().setFirstName("Vasya");
        assertEquals(client.getData().getClientData().getClient().getFirstName(), "Vasya");
    }

    @Test
    public void testMarshal() throws JAXBException, IOException {
        client.getData().getClientData().getClient().setFirstName("Vasya");
        assertTrue(client.marshal().contains("Vasya"));
    }

}