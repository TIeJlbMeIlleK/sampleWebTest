package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private final String destinationCardNumber = "42965358" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationCardNumber2 = "42774455" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount = "4288" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount2 = "4288" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeINN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);

    //        TODO требуется реализовать настройку блока Alert Scoring Model по правилу + Alert Scoring Model общие настройки

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(25);

        getIC().locateTable(TABLE_QUARANTINE)
                .deleteAll();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
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
                System.out.println(dboId);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "1. Провести транзакции от имени Клиента № 1" +
                    "1.1. Перевод на карту",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getCardTransfer()
                .withDestinationCardNumber(destinationCardNumber);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);

        getIC().locateTable(TABLE_QUARANTINE)
                .setTableFilter("Номер Карты получателя", "Equals", destinationCardNumber)
                .refreshTable()
                .delete(2);
    }

    @Test(
            description = "1.2. Перевод через систему денежных переводов",
            dependsOnMethods = "step1"
    )
    public void step2() {

        Transaction transaction = getMTSystemTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getMTSystemTransfer()
                .withReceiverName("Иванов Иван Иванович")
                .withReceiverCountry("РОССИЯ");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);

        getIC().locateTable(TABLE_QUARANTINE)
                .setTableFilter("Наименование получателя в системе денежных переводов", "Equals", "Иванов Иван Иванович")
                .addTableFilter("1", "Страна получателя в системе денежных переводов", "Equals", "РОССИЯ")
                .refreshTable()
                .delete(2);
    }

    @Test(
            description = "1.3. Изменение перевода, отправленного через систему денежных переводов (изменение перевода 1.2) с изменением получателя",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getMTSystemTransferEdit();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getMTTransferEdit()
                .getSystemTransferCont().withReceiverName("Григорьев Николай Петрович")
                .withReceiverCountry("США");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);


        getIC().locateTable(TABLE_QUARANTINE)
                .setTableFilter("Наименование получателя в системе денежных переводов", "Equals", "Григорьев Николай Петрович")
                .addTableFilter("1", "Страна получателя в системе денежных переводов", "Equals", "США")
                .refreshTable()
                .delete(2);
    }

    @Test(
            description = "1.4. Перевод на счет другому лицу",
            dependsOnMethods = "step3"
    )
    public void step4() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(payeeAccount)
                .withPayeeINN(payeeINN)
                .withPayeeName("Иванов Иван Иванович");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);

        getIC().locateTable(TABLE_QUARANTINE)
                .setTableFilter("Имя получателя", "Equals", "Иванов Иван Иванович")
                .addTableFilter("1", "ИНН получателя", "Equals", payeeINN)
                .addTableFilter("2", "Номер банковского счета получателя", "Equals", payeeAccount)
                .refreshTable()
                .delete(2);
    }

    @Test(
            description = "1.5 Перевод по номеру телефона",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionTELEPHON_VALUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withPayeePhone("79250957202")
                .withDestinationCardNumber(destinationCardNumber2)
                .withBIK("421569877")
                .withPayeeAccount(payeeAccount2)
                .withPayeeName("Андрей");
        sendAndAssert(transaction);

        getIC().locateTable(TABLE_QUARANTINE)
                .setTableFilter("Имя получателя", "Equals", "Андрей")
                .addTableFilter("1", "Номер банковского счета получателя", "Equals", payeeAccount2)
                .addTableFilter("2", "БИК банка получателя", "Equals", "421569877")
                .addTableFilter("3", "Номер Карты получателя", "Equals", destinationCardNumber2)
                .addTableFilter("4", "Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", "Equals", "79250957202")
                .refreshTable()
                .delete(2);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        return transaction;
    }

    private Transaction getMTSystemTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/MT_SYSTEM_TRANSFER_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getMTSystemTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        return transaction;
    }

    private Transaction getMTSystemTransferEdit() {
        Transaction transaction = getTransaction("testCases/Templates/MT_TRANSFER_EDIT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getMTTransferEdit()
                .getSystemTransferCont()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        return transaction;
    }

    private Transaction getTransactionTELEPHON_VALUE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(100));
        return transaction;
    }
}
