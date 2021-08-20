package ru.iitdgroup.tests.cases.BIQ_7739;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
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
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar[] birthdates = new GregorianCalendar[2];
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}, {"Сергей", "Кипитов", "Игоревич"}};

    @Test(
            description = "Включаем правило и выполняем преднастройки"
    )
    public void enableRule() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Полный возраст:", "18")
                .save()
                .detachDelGroup()
                .attachTransactionNew("Заявка на выпуск карты")
                .sleep(25);
        getIC().close();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRule"
    )
    public void addClients() {
        try {
            //birthdates[0] = new GregorianCalendar(1990, 1, 1);//можно вписать вручную дату рождения
            //birthdates[1] = new GregorianCalendar(1992, 1, 1);
            // GregorianCalendar new16yoTime = (GregorianCalendar) time.clone();//клонирует дату
            //  new16yoTime.add(Calendar.YEAR, -16);//отнимает от текущей даты 16 лет
            birthdates[0] = (GregorianCalendar) time.clone();
            birthdates[1] = (GregorianCalendar) time.clone();
            birthdates[0].add(Calendar.YEAR, -19);
            birthdates[1].add(Calendar.YEAR, -15);
            for (int i = 0; i < 2; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .withBirthDate(new XMLGregorianCalendarImpl(birthdates[i]))
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
            description = "Отправить транзакцию №1 \"Перевод по номеру телефона\"",
            dependsOnMethods = "addClients"
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
            description = "Отправить транзакцию №2  \"Заявка на выпуск карты\" " +
                    "от Клиента возраст котого младше 18 лет(16 лет), productName = цифровая",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionREQUEST_CARD_ISSUE();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getRequestCardIssue()
                .withProductName("цифровая");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, "Тип транзакции «Заявка на выпуск карты» (тип карты «цифровая»)");
    }

    @Test(
            description = "Отправить транзакцию №3  \"Заявка на выпуск карты\" от " +
                    "Клиента возраст котого старше 18 лет (20 лет), productName = виртуальная",
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
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
