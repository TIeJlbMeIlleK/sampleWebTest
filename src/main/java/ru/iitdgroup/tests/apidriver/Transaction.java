package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.transaction.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.transaction.SendTransactionDataRequestType;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class Transaction extends Template<SendTransactionDataRequestType> {

    public Transaction(String fileName) throws JAXBException, IOException {
        super(fileName);
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
