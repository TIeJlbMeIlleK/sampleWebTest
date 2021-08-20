package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_03_EntrustedTransfer extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private static final String TABLE_TRUSTED = "(Rule_tables) Доверенные получатели";
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        System.out.println("ТК №3(87) - Правило WR_03 не срабатывает для непроверяемых типов транзакций" +
                " и при отсутствии доверенного получателя по транзакции");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "5")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .sleep(15);

        getIC().locateTable(TABLE_TRUSTED)
                .deleteAll();
    }

    @Test(
            description = "Генерация клиентов",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
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
            description = "Провести транзакцию 'Перевод в бюджет'",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionBUDGET_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Провести транзакцию 'Перевод между своими счетами'",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransferBetweenAccounts();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применяется для транзакций такого типа");
    }

    @Test(
            description = "Провести транзакции, получатели для которых не находятся в списке 'Доверенные получатели':'Оплата услуг'",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSERVICE_PAYMENT();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Test(
            description = "Перевод на счет другому лицу",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Test(
            description = "Перевод на карту",
            dependsOnMethods = "transaction4"
    )
    public void transaction5() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionCARD_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Test(
            description = "Перевод через систему дененжных переводов",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Test(
            description = "Изменение перевода, отправленного через систему дененжных переводов",
            dependsOnMethods = "transaction6"
    )
    public void transaction7() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionSDP_Refactor();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Test(
            description = "Перевод по номеру телефона",
            dependsOnMethods = "transaction7"
    )
    public void transaction8() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, EMPTY_WHITE_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionBUDGET_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/BUDGET_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSERVICE_PAYMENT() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSDP() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionSDP_Refactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransferBetweenAccounts() {
        Transaction transaction = getTransaction("testCases/Templates/TRANSFER_BETWEEN_ACCOUNTS.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
