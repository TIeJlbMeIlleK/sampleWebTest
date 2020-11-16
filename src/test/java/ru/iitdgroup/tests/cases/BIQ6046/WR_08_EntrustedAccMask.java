package ru.iitdgroup.tests.cases.BIQ6046;

import com.google.i18n.phonenumbers.Phonenumber;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.openqa.selenium.ElementNotInteractableException;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.ContactChannelType;
import ru.iitdgroup.intellinx.dbo.client.ContactInfoType;
import ru.iitdgroup.intellinx.dbo.client.ContactKind;
import ru.iitdgroup.intellinx.dbo.client.ContactType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;
import ru.iitdgroup.tests.webdriver.ic.ICXPath;
import ru.iitdgroup.tests.webdriver.referencetable.Table;
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class WR_08_EntrustedAccMask extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_WR_08_EntrustedAccMask";
    private static final String PAYMENT_MASK = "464512";
    private static final String WRONG_PAYMENT_MASK = "464515";

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 11, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Маски счетов")
                .attachAddingValue("Маски счетов", "Маски счетов доверенных получателей", "Equals", PAYMENT_MASK)
                .sleep(3);
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(5);

    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
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
                        .getClientIds()
                        .withDboId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить транзакцию №1 Перевод между счетами где первые цифры SourceProduct = 464512",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        String oldSourceProduct = transactionData.getTransferBetweenAccounts().getSourceProduct();
        transactionData
                .getTransferBetweenAccounts()
                .setSourceProduct(PAYMENT_MASK + oldSourceProduct.substring(PAYMENT_MASK.length()));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, EXISTS_MATCHES);
    }

    @Test(
            description = "Отправить транзакцию №2 Перевод между счетами где первые цифры SourceProduct = 464515",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        String oldSourceProduct = transactionData.getTransferBetweenAccounts().getSourceProduct();
        transactionData
                .getTransferBetweenAccounts()
                .setSourceProduct(WRONG_PAYMENT_MASK + oldSourceProduct.substring(WRONG_PAYMENT_MASK.length()));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES);
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }


    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
