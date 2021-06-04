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

public class PayeeToWhiteList extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Степан", "Михалков", "Михайлович"}};
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String CONSOLIDATE_ACCOUNT = "(Rule_tables) Сводные счета";
    private static final String QUARANTINE_LIST = "(Rule_tables) Карантин получателей";
    private static final String TRUSTED_RECIPIENTS = "(Rule_tables) Доверенные получатели";
    private final String payeeAccountCons = "4275678" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount2 = "4158422" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeINN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String serviceName = "Мегафон по номеру телефона";
    private final String providerName = "Мегафон";
    private final String phoneNumber = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String destinationCardNumber = "4228433" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String bik = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String bik1 = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private final String payeePhone = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
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
            description = "1. Провести транзакции: Оплата услуг, Перевод на карту, Перевод на счёт (в пользу юридического лица) " +
                    "не на сводный счёт, Перевод на счёт, сводный, где ИНН 12 символов",
            dependsOnMethods = "addClients"
    )

    public void transaction1() {
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

        time.add(Calendar.SECOND, 20);
        Transaction transactionTwo = getTransactionCARD();
        TransactionDataType transactionDataCard = transactionTwo.getData().getTransactionData();
        transactionDataCard
                .getCardTransfer()
                .withDestinationCardNumber(destinationCardNumber);
        sendAndAssert(transactionTwo);

        time.add(Calendar.SECOND, 20);
        Transaction transactionThree = getOuterTransfer();
        TransactionDataType transactionDataOuter = transactionThree.getData().getTransactionData();
        transactionDataOuter
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeName("Иванов Иван Иванович")
                .withPayeeAccount(payeeAccount2);
        transactionDataOuter
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik);
        sendAndAssert(transactionThree);

        time.add(Calendar.SECOND, 20);
        Transaction transactionOuter = getOuterTransfer();
        TransactionDataType transactionDataOuterTr = transactionOuter.getData().getTransactionData();
        transactionDataOuterTr
                .getOuterTransfer()
                .getPayeeProps()
                .withPayeeINN(payeeINN)
                .withPayeeAccount(payeeAccountCons);
        transactionDataOuterTr
                .getOuterTransfer()
                .getPayeeBankProps()
                .withBIK(bik1);
        sendAndAssert(transactionOuter);

        time.add(Calendar.SECOND, 20);
        Transaction transactionPhone = getTransactionPHONE();
        TransactionDataType transactionDataPhone = transactionPhone.getData().getTransactionData();
        transactionDataPhone
                .getPhoneNumberTransfer()
                .withPayeePhone(payeePhone)
                .withPayeeName(payeeName);
        sendAndAssert(transactionPhone);

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", "Иванов Иван Иванович")
                .match("Номер банковского счета получателя", payeeAccount2)
                .match("БИК банка получателя", bik)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Номер Карты получателя", destinationCardNumber)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("ИНН сводный", payeeINN)
                .match("Номер банковского счета получателя", payeeAccountCons)
                .match("БИК банка получателя", bik1)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", payeeName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", payeePhone)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Название сервис провайдера", serviceName)
                .match("Наименование провайдера сервис услуги", providerName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", phoneNumber)
                .failIfNoRows();//проверка справочника на наличие записи
    }

    @Test(
            description = "Запустить джоб PayeeToWhiteList и проверить карантин получателей",
            dependsOnMethods = "transaction1"
    )
    public void runJob() {
        getIC().locateJobs()
                .selectJob("PayeeToWhiteList")
                .waitSeconds(10)
                .waitStatus(JobRunEdit.JobStatus.SUCCESS)
                .run();
        getIC().home();

        getIC().locateTable(QUARANTINE_LIST)//проверка наличия записи в карантине
                .refreshTable()
                .tableNoRecords();
        System.out.println("Справочник «Карантин получателей» пуст");

        getIC().locateTable(TRUSTED_RECIPIENTS)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Наименование провайдера сервис услуги", providerName)
                .match("Наименование сервиса", serviceName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", phoneNumber)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(TRUSTED_RECIPIENTS)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", "Иванов Иван Иванович")
                .match("Номер банковского счета получателя", payeeAccount2)
                .match("БИК банка получателя", bik)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(TRUSTED_RECIPIENTS)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Номер карты получателя", destinationCardNumber)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(TRUSTED_RECIPIENTS)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("ИНН сводный", payeeINN)
                .match("Номер банковского счета получателя", payeeAccountCons)
                .match("БИК банка получателя", bik1)
                .failIfNoRows();//проверка справочника на наличие записи

        getIC().locateTable(TRUSTED_RECIPIENTS)//проверка наличия записи в карантине
                .refreshTable()
                .findRowsBy()
                .match("Имя получателя", payeeName)
                .match("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером", payeePhone)
                .failIfNoRows();//проверка справочника на наличие записи
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
                .withRegular(false);
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
