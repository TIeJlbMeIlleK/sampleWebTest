package ru.iitdgroup.tests.apidriver;

import ru.iitdgroup.intellinx.crosschannel.tranantifraudcheckrequest.ObjectFactory;
import ru.iitdgroup.intellinx.crosschannel.tranantifraudcheckrequest.TranAntiFraudCheckType;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class TransactionEspp extends Template<TranAntiFraudCheckType> {

    public TransactionEspp(String fileName) throws JAXBException, IOException {
        super(fileName);
    }

    @Override
    protected Class getObjectFactoryClazz() {
        return ObjectFactory.class;
    }
}
