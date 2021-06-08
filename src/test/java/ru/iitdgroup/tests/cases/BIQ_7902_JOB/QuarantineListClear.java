package ru.iitdgroup.tests.cases.BIQ_7902_JOB;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.jobconfiguration.JobRunEdit;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class QuarantineListClear extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}, {"Артем", "Носов", "Игоревич"}};
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String CONSOLIDATE_ACCOUNT = "(Rule_tables) Сводные счета";
    private static final String QUARANTINE_LIST = "(Rule_tables) Карантин получателей";
    private static final String TRUSTED_RECIPIENTS = "(Rule_tables) Доверенные получатели";
    private static final String BLACK_INN_CONS = "(Rule_tables) Запрещенные получатели Сводный ИНН";
    private static final String BLACK_NAME_CONS = "(Rule_tables) Запрещенные получатели Сводный ФИО получателя";
    private static final String BLACK_INN = "(Rule_tables) Запрещенные получатели ИНН";
    private static final String BLACK_CARD = "(Rule_tables) Запрещенные получатели НомерКарты";
    private static final String GRAY_BIK_ACCOUNT = "(Rule_tables) Подозрительные получатели БИКСЧЕТ";
    private static final String GRAY_PHONE = "(Rule_tables) Подозрительные получатели телефон";
    private static final String BLACK_PHONE = "(Rule_tables) Запрещенные получатели НомерТелефона";
    private final String payeeAccountCons = "42756789" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccountCons1 = "42756758" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String nameRecipient = "Иванов Иван Иванович";
    private final String payeeAccount2 = "41584229" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount = "41384777" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeINNCons = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeINN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String serviceName = "Мегафон по номеру телефона";
    private final String providerName = "Мегафон";
    private final String phoneNumber = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String destinationCardNumber = "4228433" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String destinationCardNumber1 = "4228455" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String bik = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String bik1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String bik2 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String bik3 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String payeePhoneBlack = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String payeeName = "Андрей";

    @Test(
            description = "Включить правило; остальные правила деактивированы"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(2)
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("код значения", "CLAIM_PERIOD")
                .click()
                .edit()
                .fillInputText("Значение:", "0")
                .save();
        getIC().locateTable(CONSOLIDATE_ACCOUNT)
                .deleteAll()
                .addRecord()
                .fillInputText("Маска счёта:", "42756")
                .save();

        getIC().locateTable(QUARANTINE_LIST).deleteAll();
        getIC().locateTable(TRUSTED_RECIPIENTS).deleteAll();
    }

    @Test(
            description = "Создаем клиентов",
            dependsOnMethods = "enableRules"
    )
    public void addClients() {
        try {
            for (int i = 0; i < 2; i++) {
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
            description = "1. Провести транзакции: Оплата услуг, Перевод на карту, Перевод на счёт (в пользу юридического лица) " +
                    "не на сводный счёт, Перевод на счёт, сводный, где ИНН 12 символов и внести данные в Черные и серые справочники",
            dependsOnMethods = "addClients"
    )

    public void transaction() {
        time.add(Calendar.MINUTE, -20);
        Transaction transaction = getTransactionServicePayment();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getServicePayment()
                .withProviderName(providerName)
                .withServiceName(serviceName)
                .getAdditionalField()
                .get(0)
                .withValue(phoneNumber);
        sendAndAssert(transaction);

        getIC().locateTable(GRAY_PHONE)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер телефона:", phoneNumber)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionCARD();
        TransactionDataType transactionDataTwo = transactionTwo.getData().getTransactionData();
        transactionDataTwo
                .getCardTransfer()
                .withDestinationCardNumber(destinationCardNumber);
        sendAndAssert(transactionTwo);

        getIC().locateTable(BLACK_CARD)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер карты:", destinationCardNumber)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionThree = getOuterTransfer();
        TransactionDataType transactionDataThree = transactionThree.getData().getTransactionData();
        transactionDataThree
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeName(nameRecipient)
                .withPayeeAccount(payeeAccountCons1);
        transactionDataThree
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik);
        sendAndAssert(transactionThree);

        getIC().locateTable(BLACK_NAME_CONS)
                .deleteAll()
                .addRecord()
                .fillInputText("БИК:", bik)
                .fillInputText("ФИО получателя:", nameRecipient)
                .fillInputText("Сводный счет:", payeeAccountCons1)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionOuterCons = getOuterTransfer();
        TransactionDataType transactionDataOuterCons = transactionOuterCons.getData().getTransactionData();
        transactionDataOuterCons
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionDataOuterCons
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeINN(payeeINNCons)
                .withPayeeAccount(payeeAccountCons);
        transactionDataOuterCons
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik1);
        sendAndAssert(transactionOuterCons);

        getIC().locateTable(BLACK_INN_CONS)
                .deleteAll()
                .addRecord()
                .fillInputText("БИК:", bik1)
                .fillInputText("ИНН:", payeeINNCons)
                .fillInputText("Сводный счет:", payeeAccountCons)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionPhone = getTransactionPHONE();
        TransactionDataType transactionDataPhone = transactionPhone.getData().getTransactionData();
        transactionDataPhone
                .getPhoneNumberTransfer()
                .withPayeePhone(payeePhoneBlack)
                .withPayeeName(payeeName);
        sendAndAssert(transactionPhone);

        getIC().locateTable(BLACK_PHONE)
                .deleteAll()
                .addRecord()
                .fillInputText("Номер телефона:", payeePhoneBlack)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionOuterINN = getOuterTransfer();
        TransactionDataType transactionDataOuterINN = transactionOuterINN.getData().getTransactionData();
        transactionDataOuterINN
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeINN(payeeINN)
                .withPayeeAccount(payeeAccount);
        transactionDataOuterINN
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik2);
        sendAndAssert(transactionOuterINN);

        getIC().locateTable(BLACK_INN)
                .deleteAll()
                .addRecord()
                .fillInputText("ИНН:", payeeINN)
                .save();

        time.add(Calendar.SECOND, 20);
        Transaction transactionOuter = getOuterTransfer();
        TransactionDataType transactionDataOuterTr = transactionOuter.getData().getTransactionData();
        transactionDataOuterTr
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeAccount(payeeAccount2);
        transactionDataOuterTr
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik3);
        sendAndAssert(transactionOuter);

        getIC().locateTable(GRAY_BIK_ACCOUNT)
                .deleteAll()
                .addRecord()
                .fillInputText("БИК:", bik3)
                .fillInputText("Счет:", payeeAccount2)
                .save();

//        time.add(Calendar.SECOND, 20);
//        Transaction transactionCard = getTransactionCARD();
//        TransactionDataType transactionDataCard = transactionCard.getData().getTransactionData();
//        transactionDataCard
//                .getCardTransfer()
//                .withDestinationCardNumber(destinationCardNumber1);
//        sendAndAssert(transactionCard);

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Название сервис провайдера", serviceName)
                .match("Наименование провайдера сервис услуги", providerName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", phoneNumber)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Номер Карты получателя", destinationCardNumber)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("ФИО получателя сводное", nameRecipient)
                .match("Номер банковского счета получателя", payeeAccountCons1)
                .match("БИК банка получателя", bik)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("ИНН сводный", payeeINNCons)
                .match("Номер банковского счета получателя", payeeAccountCons)
                .match("БИК банка получателя", bik1)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", payeeName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", payeePhoneBlack)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("ИНН получателя", payeeINN)
                .match("Номер банковского счета получателя", payeeAccount)
                .match("БИК банка получателя", bik2)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Номер банковского счета получателя", payeeAccount2)
                .match("БИК банка получателя", bik3)
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Test(
            description = "Запустить джоб QuarantineListClear и проверить карантин получателей",
            dependsOnMethods = "transaction"
    )
    public void runJob() {
        getIC().locateJobs()
                .selectJob("QuarantineListClear")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .tableNoRecords();
        System.out.println("Справочник «Карантин получателей» пуст");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(1L)
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getServicePayment()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000));
        return transaction;
    }

    private Transaction getTransactionCARD() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(1L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }

    private Transaction getOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER_Android.xml");
        transaction.getData()
                .getServerInfo()
                .withPort(8050);
        transaction.getData().getTransactionData()
                .getClientIds()
                .withDboId(clientIds.get(0));
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000.00))
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500.00));
        return transaction;
    }

    private Transaction getTransactionPHONE() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER_IOS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time))
                .withRegular(false)
                .withVersion(1L);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        return transaction;
    }
}
