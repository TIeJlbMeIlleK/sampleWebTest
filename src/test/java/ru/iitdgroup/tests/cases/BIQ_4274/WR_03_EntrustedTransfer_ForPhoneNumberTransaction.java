package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
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

public class WR_03_EntrustedTransfer_ForPhoneNumberTransaction extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private static final String GOOD_PAYEE = "(Rule_tables) Доверенные получатели";
    private static final String GOOD_CARD = "4712503155771158";
    private static final String GOOD_PHONE = "9412574781";
    private static final String GOOD_BIK = "042307434";
    private static final String GOOD_ACCOUNT = "4081710835650123456";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.AUGUST, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("Правило WR_03 срабатывает при наличии получателя в доверенных" + "ТК №46");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:","5000")
                .fillInputText("Период серии в минутах:","5")
                .fillInputText("Статистический параметр Обнуления (0.95):","0,95")
                .save()
                .sleep(5);
    }

    @Test(
            description = "Генерация клиентов",
            dependsOnMethods = "enableRules"
    )
    public void client() {
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Добавление доверенных получателей по клиентам",
            dependsOnMethods = "client"
    )
    public void GoodPayeeForClient() {
        Table.Formula rows1 = getIC().locateTable(GOOD_PAYEE).findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable(GOOD_PAYEE)
                .addRecord()
                .fillUser("Клиент:",clientIds.get(0))
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора  с сервис провайдером:",GOOD_PHONE)
                .fillInputText("Номер карты получателя:",GOOD_CARD)
                .fillInputText("БИК банка получателя:",GOOD_BIK)
                .fillInputText("Номер банковского счета получателя:",GOOD_ACCOUNT)
                .save();
        getIC().close();
        }

    @Test(
            description = "Провести транзакции № 1 \"Перевод по номеру телефона\" от клиента №1  -- номер телефона в доверенных(\"БИК+ СЧЕТ\" и \"Номер карты\" нет)",
            dependsOnMethods = "GoodPayeeForClient"
    )
    public void transaction1() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone(GOOD_PHONE);
        transactionData.getPhoneNumberTransfer()
                .setBIK("042301111");
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount("4081710835650000333");
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4712583155775555");
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);
    }

    @Test(
            description = "Провести транзакции № 2 \"Перевод по номеру телефона\" от клиента №1  -- БИК+ СЧЕТ в доверенных (\"Номер телефона\" и \"Номер карты\" нет)",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("9514475231");
        transactionData.getPhoneNumberTransfer()
                .setBIK(GOOD_BIK);
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount(GOOD_ACCOUNT);
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber("4712583155775555");
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);
    }

    @Test(
            description = "Провести транзакции № 3 \"Перевод по номеру телефона\" от клиента №1  -- Номер карты в доверенных (\"Номер телефона\" и \"БИК+ СЧЕТ\" нет)",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getPhoneNumberTransfer()
                .setPayeePhone("9514475231");
        transactionData.getPhoneNumberTransfer()
                .setBIK("042301111");
        transactionData.getPhoneNumberTransfer()
                .setPayeeAccount("4081710835650000222");
        transactionData.getPhoneNumberTransfer()
                .setDestinationCardNumber(GOOD_CARD);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_IN_WHITE_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
