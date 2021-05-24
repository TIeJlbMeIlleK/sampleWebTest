package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.ContactChannelType;
import ru.iitdgroup.intellinx.dbo.client.ContactKind;
import ru.iitdgroup.intellinx.dbo.client.ContactType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

//TODO перед запуском теста должен быть установлен SECRET KEY

public class InformClientOfTransactionWithSMS extends RSHBCaseTest {

    private static final String RULE_NAME = "";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Шаблоны СМС";
    private static final String SMS_TEMPLATE =
            "RSHB operatsiya na summu ${tx_amount} ${tx_isoCurCode} ${tx_isoCountryCode} priostanovlena. " +
                    "Dlya podtverzhdeniya pozvonite v bank po nomeru 88001000100 ili 84957265646 libo dozhdites zvonka";
    private static final String REFERENCE_ITEM2 = "(System_parameters) Интеграционные параметры";
    private static final String AUTH_PHONE = "79250252525";
    private static final String NOTIFICATION_PHONE = "79250333333";

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Павел", "Петушков", "Павлович"}};
    private static String[] login = {new RandomString(5).nextString()};
    private static String[] loginHash = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};
    private static String[] dboId = {(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5)};

    @Test(
            description = "Включить все правила и установить cutting score 1"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .activate()
                .sleep(15);
        getIC().locateScoringModels()
                .openRecord("Подозрительная транзакция")
                .edit()
                .fillInputText("Cutting score:", "1")
                .save();
    }

    @Test(
            description = " Создать в справочнике \"Шаблоны СМС\" шаблон для отправки СМС, " +
                    "Привязать шаблон к статусу Complete, резолюции CONTINUE и  включить LOG_SMS = 1",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.click().edit()
                    .fillInputText("AlertStatus:", "Complete")
                    .fillInputText("AlertResolution:", "CONTINUE")
                    .fillInputText("Template:", SMS_TEMPLATE)
                    .save();
        } else {

            getIC().locateTable(REFERENCE_ITEM)
                    .addRecord()
                    .fillInputText("AlertStatus:", "Complete")
                    .fillInputText("AlertResolution:", "CONTINUE")
                    .fillInputText("Template:", SMS_TEMPLATE)
                    .save();
        }
        getIC().locateTable(REFERENCE_ITEM2)
                .findRowsBy()
                .match("Описание", "Логирование сообщений SMS шлюза (in/out)")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Создаем клиента и меняем телефон для уведомления" +
                    "(указываем новый отличный от аутентификации, если уже был указан)",
            dependsOnMethods = "editReferenceData"
    )
    public void addClient() {
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
            description = "Отправить транзакцю. Открыть алерт и выполнить созданный Action (отправить СМС)",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        String lastSMSId = getLastSentSMSInformation()[0];
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("SEND_SMS")
                .sleep(2);

        getIC().close();

        String[] lastSMS = getLastSentSMSInformation(); //в переменной информация о последней переданной СМС
        Assert.assertNotEquals(lastSMSId, lastSMS[0]);//проверка отличия ID от предыдущего отправленного СМС
        assertEquals(NOTIFICATION_PHONE, lastSMS[5]);//проверка отправки смс по номеру телефона
        String SMStext = lastSMS[3];//текст смс
        assertFalse(SMStext.contains("${tx_amount}"));//проверка смс, что в ней не содержатся такие строки
        assertFalse(SMStext.contains("${tx_isoCurCode}"));
        assertFalse(SMStext.contains("${tx_isoCountryCode}"));

        /*
        RSHB operatsiya na summu 500.00 RUR RUS priostanovlena. Dlya podtverzhdeniya pozvonite v bank po nomeru 88001000100 ili 84957265646 libo dozhdites zvonka
        RSHB operatsiya na summu ${tx_amount} ${tx_isoCurCode} ${tx_isoCountryCode} priostanovlena.
         */
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private void updatePhones(Client client) {
        List<ContactType> contacts = client.getData().getClientData()
                .getContactInfo()
                .getContact();

        boolean authExist = false;
        boolean notificExist = false;
        for (ContactType contact : contacts) {
            if (contact.getContactChannel() == ContactChannelType.PHONE &&
                    contact.getContactKind() == ContactKind.AUTH) {
                contact.setValue(AUTH_PHONE);
                authExist = true;
                break;
            }
        }
        if (!authExist) {
            contacts.add(new ContactType()
                    .withContactChannel(ContactChannelType.PHONE)
                    .withContactKind(ContactKind.AUTH)
                    .withValue(AUTH_PHONE));
        }
        for (ContactType contact : contacts) {
            if (contact.getContactChannel() == ContactChannelType.PHONE &&
                    contact.getContactKind() == ContactKind.NOTIFICATION) {
                contact.setValue(NOTIFICATION_PHONE);
                notificExist = true;
                break;
            }
        }
        if (!notificExist) {
            contacts.add(new ContactType()
                    .withContactChannel(ContactChannelType.PHONE)
                    .withContactKind(ContactKind.NOTIFICATION)
                    .withValue(NOTIFICATION_PHONE));
        }
    }
}
