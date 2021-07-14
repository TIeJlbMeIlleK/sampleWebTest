package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee_LastTransaction extends RSHBCaseTest {

    private static final String TABLE_QUARANTINE = "(Rule_tables) Карантин получателей";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private final GregorianCalendar time = new GregorianCalendar();
    private final GregorianCalendar time1 = new GregorianCalendar();
    private GregorianCalendar time2;
    private final List<String> clientIds = new ArrayList<>();
    private static final String LOCAL_TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";
    private final String[][] names = {{"Татьяна", "Спиридонова", "Георгиевна"}};
    private final String destantionCardNumber = "425678" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final String payeeAccount = "425678" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final String QUARANTINE_RECIPIENT = "Олеся Викторовна Зимина";

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

        getIC().locateTable(LOCAL_TABLE)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .click()
                .edit()
                .fillInputText("Значение:", "1")
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
        time1.add(Calendar.HOUR, -50);
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

        getIC().locateTable(TABLE_QUARANTINE)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", QUARANTINE_RECIPIENT)
                .fillInputText("Номер Карты получателя:", destantionCardNumber)
                .fillInputText("Номер банковского счета получателя:", payeeAccount)
                .fillInputText("Дата занесения:", format.format(time1.getTime()))//дата занесения 2 дня назад
                .fillInputText("Дата последней авторизованной транзакции:", format.format(time1.getTime()))
                .save();
    }

    @Test(
            description = "1. Провести транзакцию № 1 Перевод по номеру телефона",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time1.add(Calendar.HOUR, 2);
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time1))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time1));
        transactionData
                .getPhoneNumberTransfer()
                .withBIK(null)
                .withPayeeName(QUARANTINE_RECIPIENT)
                .withPayeeAccount(payeeAccount)
                .withDestinationCardNumber(destantionCardNumber)
                .withPayeePhone(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Получатель недавно находится в карантине");
    }

    @Test(
            description = "3.  Провести транзакцию № 2 'Перевод по номеру телефона' от имени клиенат № 1 в пользу получателя, находящегося в карантине",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, -10);
        time2 = (GregorianCalendar) time.clone();
        Transaction transaction = getTransactionPhoneNumberTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPhoneNumberTransfer()
                .withBIK(null)
                .withPayeeName(QUARANTINE_RECIPIENT)
                .withPayeeAccount(payeeAccount)
                .withDestinationCardNumber(destantionCardNumber)
                .withPayeePhone(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(FEW_DATA, RESULT_EXIST_QUARANTINE_LOCATION);
    }

    @Test(
            description = "4. Проверить Карантин получателей",
            dependsOnMethods = "step2"
    )
    public void step3() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String s = dateFormat.format(time2.getTime());

        getIC().locateTable(TABLE_QUARANTINE)
                .findRowsBy()
                .match("Номер Карты получателя", destantionCardNumber)
                .click();
        assertTableField("Дата последней авторизованной транзакции:", s);
        getIC().close();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPhoneNumberTransfer() {
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
}
