package ru.iitdgroup.tests.apidriver;

import generated.TranAntiFraudCheckResponseType;
import ru.iitdgroup.intellinx.crosschannel.tranantifraudcheckrequest.TranAntiFraudCheckType;
import ru.iitdgroup.intellinx.dbo.sendsubstransactiondatarequest.SendSubsTransactionDataRequestType;
import ru.iitdgroup.intellinx.dbo.transaction.ObjectFactory;
import ru.iitdgroup.intellinx.dbo.transaction.SendTransactionDataRequestType;

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
