package ru.iitdgroup.rshbtest;


import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Authentication;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.DBOAntiFraudWS;
import ru.iitdgroup.tests.apidriver.Transaction;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WSTests extends RSHBTests {

    private DBOAntiFraudWS ws;

    @BeforeMethod
    public void before() {
        ws = new DBOAntiFraudWS(props.getWSUrl(), props.getWSUser(), props.getWSPassword());
    }

    @Test(
            priority = 0,
            description = "Пример заполнения атрибутов аутентификации и отправки их на антифрод-сервис"
    )
    public void callAuth() throws Exception {
        Authentication authentication = new Authentication("auth/auth1.xml");

        ws.send(authentication);

        assertEquals(200, ws.getResponseCode().intValue());
        assertTrue(ws.isSuccessResponse());
    }

    @Test(
            priority = 1,
            description = "Пример заполнения атрибутов клиента и отправки их на антифрод-сервис"
    )
    public void callClient() throws Exception {
        Client client = new Client("clients/client1.xml");

        ws.send(client);

        assertEquals(200, ws.getResponseCode().intValue());
        assertTrue(ws.isSuccessResponse());
    }

    @Test(
            priority = 2,
            description = "Пример заполнения атрибутов транзакции и отправки их на антифрод-сервис"
    )
    public void callTransaction() throws Exception {
        Transaction transaction = new Transaction("transactions/tran1.xml");
        transaction.getData().getTransactionData().withTransactionId("5000000" + new Random().nextInt(100000));

        ws.send(transaction);

        assertEquals(200, ws.getResponseCode().intValue());
        assertTrue(ws.isSuccessResponse());
    }
}
