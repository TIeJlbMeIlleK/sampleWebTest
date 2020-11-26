package ru.iitdgroup.tests.cases.BIQ_6046;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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


public class NoteSetAttentionAUTH extends RSHBCaseTest {

    private static final String RULE_NAME = "";
    private static final String REFERENCE_ITEM = "(System_tables) Действия по событиям от Сотовых операторов";
    private static final String AUTH_PHONE_MTS = "+79101001010";
    private static final String AUTH_PHONE_VIMPEL = "+79202002020";
    private static final String AUTH_PHONE_TELE2 = "+79303003030";
    private static final String AUTH_PHONE_MEGAFON = "+79104004040";


    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.NOVEMBER, 1, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "В справочнике «Действия по событиям от Сотовых операторов» добавлены запись по каждому оператору:\n" +
                    "-- «Установить признак «Особое внимание», с любым, соответствующим оператору кодом события\n" +
                    "-- Active = True"

    )

    public void editReferenceData() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM).findRowsBy();

        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillFromExistingValues("Код события:", "Description", "Equals", "\"Блокировка по утере или краже")
                .select("Реакция:", "SET_ATTENTION")
                .fillCheckBox("Действие включено:", true)
                .save();
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillFromExistingValues("Код события:", "Description", "Equals", "Заключительная блокировка")
                .select("Реакция:", "SET_ATTENTION")
                .fillCheckBox("Действие включено:", true)
                .save();
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillFromExistingValues("Код события:", "Description", "Equals", "Блокировка перед расторжением (MNP)")
                .select("Реакция:", "SET_ATTENTION")
                .fillCheckBox("Действие включено:", true)
                .save();
        getIC().locateTable(REFERENCE_ITEM)
                .addRecord()
                .fillFromExistingValues("Код события:", "Description", "Equals", "Телефон заражен")
                .select("Реакция:", "SET_ATTENTION")
                .fillCheckBox("Действие включено:", true)
                .save();
        getIC().close();
    }


    @Test(
            description = "Создать по одному клиенту от каждого оператора связи с Активными телефономи в роли AUTH",
            dependsOnMethods = "editReferenceData"
    )
    public void step0() {
        try {
            for (int i = 0; i < 4; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client.getData()
                        .getClientData()
                        .getClient()
                        .getClientIds()
                        .withDboId(dboId);

                for (ContactType c : client.getData().getClientData().getContactInfo().getContact()) {
                    if (c.getContactKind().value().equals("AUTH") && i == 0) {
                        c.setValue(AUTH_PHONE_MTS);
                    } else if (c.getContactKind().value().equals("AUTH") && i == 1) {
                        c.setValue(AUTH_PHONE_TELE2);
                    } else if (c.getContactKind().value().equals("AUTH") && i == 2) {
                        c.setValue(AUTH_PHONE_VIMPEL);
                    } else if (c.getContactKind().value().equals("AUTH") && i == 3) {
                        c.setValue(AUTH_PHONE_MEGAFON);
                    }
                }
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Отправить сообщение от ОСС с кодом события из предусловия по Телефонам, от соответствующих операторов",
            dependsOnMethods = "step0"
    )

    public void step1() {
//        String lastSMSId = getLastSentSMSInformation()[0];
//
//        Transaction transaction = getTransaction();
//        TransactionDataType transactionData = transaction.getData().getTransactionData()
//                .withRegular(false);
//        transactionData
//                .getClientIds()
//                .withDboId(clientIds.get(0));
//
//        sendAndAssert(transaction);
//
//        getIC().locateAlerts()
//                .openFirst()
//                .action("SEND_SMS")
//                .sleep(2);
//
//        getIC().locateReports()
//                .openFolder("Системные отчеты")
//                .openFolder("Логированные сообщения");//визуально увидеть отправку СМС
//
//        getIC().close();

        String[] lastSMS = getLastSentSMSInformation(); //в переменной информация о последней переданной СМС
        //Assert.assertNotEquals(lastSMSId, lastSMS[0]);//проверка отличия ID от предыдущего отправленного СМС
        // assertEquals(NOTIFICATION_PHONE, lastSMS[5]);//проверка отправки смс по номеру телефона
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

    private Transaction getMessage() {
        return getMessage();
    }
}
