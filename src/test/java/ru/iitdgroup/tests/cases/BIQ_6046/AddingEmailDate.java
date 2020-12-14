package ru.iitdgroup.tests.cases.BIQ_6046;

import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.*;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class AddingEmailDate extends RSHBCaseTest {

    private static final String RULE_NAME = "";
    private static final String EMAIL1 = "abrakadabra25@mail.ru";
    private static final String EMAIL2 = "abrakadabra@mail.ru";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<Client> clients = new ArrayList<>();

    @Test(
            description = "Создаем карточку нового клиента №1 с новым Email"
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
                        .withFirstName("Мария")
                        .withLastName("Иванова")
                        .withMiddleName("Васильевна")
                        .getClientIds()
                        .withDboId(dboId)
                        .withEksId("1155")
                        .withCifId("1155")
                        .withExpertSystemId("1155")
                        .withLoginHash("1155")
                        .getAlfaIds()
                        .withAlfaId("1155");

                updateEmail(client, EMAIL1);
                sendAndAssert(client);
                clients.add(client);
                System.out.println(dboId);
                assertClientEmailApply(dboId, EMAIL1);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

    }

    @Test(
            description = "Создаем карточку нового клиента №1 с новым Email",
            dependsOnMethods = "step0"
    )
    public void step1() {
        for (Client client : this.clients) {
            updateEmail(client, EMAIL2);
            sendAndAssert(client);
            String dboId = client.getData().getClientData().getClient().getClientIds().getDboId();
            assertClientEmailChanged(dboId, EMAIL2);
        }
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private void updateEmail(Client client, String email) {
        List<ContactType> contacts = client.getData().getClientData().getContactInfo().getContact();
        boolean exists = false;
        for (ContactType f : contacts) {
            if (f.getContactChannel() == ContactChannelType.EMAIL) {
                f.setValue(email);
                exists = true;
                break;
            }
        }

        if (!exists) {
            contacts.add(new ContactType()
                    .withContactChannel(ContactChannelType.EMAIL)
                    .withContactKind(ContactKind.NOTIFICATION)
                    .withValue(email)
            );
        }

    }
}
