package ru.iitdgroup.rshbtest;


import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Transaction;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Random;

import static org.testng.Assert.assertEquals;

public class WSTests extends RSHBTests {

    private DBOAntiFraudWS ws;

    @BeforeMethod
    public void before() {
        ws = new DBOAntiFraudWS(props.getWSUrl());
    }

    @Test(
            priority = 0,
            description = "Пример заполнения атрибутов аутентификации и отправки их на антифрод-сервис"
    )
    public void callAuth() throws JAXBException, IOException {
        Authentication authentication = new Authentication("auth/auth1.xml");
        ws.send(authentication);

        assertEquals(200, ws.getResponseCode().intValue());
    }

    @Test(
            priority = 1,
            description = "Пример заполнения атрибутов клиента и отправки их на антифрод-сервис"
    )
    public void callClient() throws JAXBException, IOException {
        Client client = new Client("clients/client1.xml");
        ws.send(client);

        assertEquals(200, ws.getResponseCode().intValue());
    }

    @Test(
            priority = 2,
            description = "Пример заполнения атрибутов транзакции и отправки их на антифрод-сервис"
    )
    public void callTransaction() throws IOException, JAXBException {
        Transaction t = new Transaction("transactions/tran1.xml");
        t.withTransactionId("5000000" + new Random().nextInt(100000));

        ws.send(t);

        assertEquals(200, ws.getResponseCode().intValue());
    }
}
