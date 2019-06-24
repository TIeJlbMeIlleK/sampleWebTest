package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.transaction.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.transaction.SendTransactionDataRequestType;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class Transaction extends Template<SendTransactionDataRequestType> {

    public Transaction(String fileName) throws JAXBException, IOException {
        super(fileName);
    }

    public Transaction withDBOId(String newDboId) {
        getData().getTransactionData().getClientIds().setDboId(newDboId);
        return this;
    }

    public Transaction withTransactionId(String newTransactionId) {
        getData().getTransactionData().setTransactionId(newTransactionId);
        return this;
    }

    public Transaction withCIFId(String newCIFId) {
        getData().getTransactionData().getClientIds().setCifId(newCIFId);
        return this;
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
