package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.client.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.client.SendClientDataRequestType;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class Client extends Template<SendClientDataRequestType> {

    public Client(String fileName) throws JAXBException, IOException {
        super(fileName);
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
