package ru.iitdgroup.tests.cases.BIQ_6046;

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
import ru.iitdgroup.tests.webdriver.ruleconfiguration.RuleRecord;

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
    private String[][] names = {{"Петр", "Урин", "Семенович"}, {"Ольга", "Румянцева", "Григорьевна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate();
        getIC().locateRules()
                .openRecord(RULE_NAME)
                .detachWithoutRecording("Маски счетов")
                .attachAddingValue("Маски счетов", "Маски счетов доверенных получателей", "Equals", PAYMENT_MASK)
                .sleep(30);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 6);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);

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
            dependsOnMethods = "addClients"
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
