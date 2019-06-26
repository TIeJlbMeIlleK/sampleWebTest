package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.auth.ClientAuthenticationRequestType;
import ru.iitdgroup.intellinx.dbo.auth.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class Authentication extends Template<ClientAuthenticationRequestType> {

    public Authentication(String fileName) throws JAXBException, IOException {
        super(fileName);
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
