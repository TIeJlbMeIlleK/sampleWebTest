package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_29_RequestCardIssue extends RSHBCaseTest {


    private static final String RULE_NAME = "R01_GR_29_RequestCardIssue";
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private final GregorianCalendar time_1 = new GregorianCalendar(2020, Calendar.DECEMBER, 15, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();
    private final List<String> clientId2 = new ArrayList<>();


    @Test(
            description = "Создание клиентов"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
//                client
//                        .getData().getClientData()
//                        .getClient()
//                        .withBirthDate();
                //НУЖНО УКАЗАТЬ ДАТУ чтобы клиенту было СТАРШЕ 18 ЛЕТ

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
//                client
//                        .getData().getClientData()
//                        .getClient()
//                        .withBirthDate();
                //НУЖНО УКАЗАТЬ ДАТУ чтобы клиенту было МЛАДШЕ 18 ЛЕТ
                sendAndAssert(client);
                clientId2.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включаем правило и выполняем преднастройки",
            dependsOnMethods = "createClients"
    )
    public void step0() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Полный возраст:","18")
                .fillCheckBox("Active:",true)
                .save()
                .detachAll("Типы транзакций")
                .attachTransactionNew("Типы транзакций","Заявка на выпуск карты")
                .sleep(30);
        getIC().close();
        commandServiceMock.run();
    }

    @Test(
            description = "Отправить транзакцию №1 \"Перевод по номеру телефона\"",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, ANOTHER_TRANSACTION_TYPE);
    }

    @Test(
            description = "Отправить транзакцию №2  \"Заявка на выпуск карты\" от Клиента возраст котого младше 18 лет(16 лет), productName = цифровая",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientId2.get(0));
        transactionData
                .getRequestCardIssue()
                .setProductName("цифровая");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_YOUNG_MAN);
    }

    @Test(
            description = "Отправить транзакцию №3  \"Заявка на выпуск карты\" от Клиента возраст котого старше 18 лет (20 лет), productName = виртуальная",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getRequestCardIssue()
                .setProductName("виртуальная");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_OLD_MAN);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionREQUEST_CARD_ISSUE() {
        Transaction transaction = getTransaction("testCases/Templates/REQUEST_CARD_ISSUE_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        return transaction;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time_1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time_1));
        return transaction;
    }

}
