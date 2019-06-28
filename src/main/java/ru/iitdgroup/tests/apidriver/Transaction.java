package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.dbo.transaction.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.transaction.SendTransactionDataRequestType;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class Transaction extends Template<SendTransactionDataRequestType> {

    public Transaction(String fileName) throws JAXBException, IOException {
        super(fileName);
        getData().getTransactionData()
                .withTransactionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
