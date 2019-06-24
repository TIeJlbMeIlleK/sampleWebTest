package ru.iitdgroup.rshbtest;


import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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

    @Test(description = "Пример заполнения атрибутов транзакции и отправки её на антифрод-сервис")
    public void callAntifraudWS() throws IOException, JAXBException {
        //TODO: Добавлять и удалять теги

        Random r = new Random();

        Transaction t = new Transaction("transactions/tran1.xml");
        t.withDBOId("2");
        t.withCIFId("1");
        t.withTransactionId("5000000" + r.nextInt(100000));

        ws.send(t);

        assertEquals(200, ws.getResponseCode().intValue());

        System.out.println(String.format("Response code: %d", ws.getResponseCode()));
        System.out.println(String.format("Code: %s, Error: %s, ErrorMessage: %s",
                ws.getSuccessCode(),
                ws.getErrorCode(),
                ws.getErrorMessage()));
    }

    @Test
    public void callAntifraudWSClient() throws JAXBException, IOException {
        Client client = new Client("clients/client1.xml");
        ws.send(client);

        String response = ws.getResponse();
    }
}
