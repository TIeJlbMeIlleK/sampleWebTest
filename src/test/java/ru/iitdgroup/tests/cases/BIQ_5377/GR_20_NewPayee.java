package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GR_20_NewPayee extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ульяна", "Смирнова", "Викторовна"}};
    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM1 = "(Rule_tables) Карантин получателей";
    private static final String REFERENCE_ITEM2 = "(Rule_tables) Доверенные получатели";
    private static final String TRUSTED_RECIPIENT = "Егор Ильич Иванов";
    private static final String QUARANTINE_RECIPIENT = "Киса Витальевич Емельяненко";

    @Test(
            description = "Включить правило GR_20_NewPayee"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);
        commandServiceMock.run();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
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

        getIC().locateTable(REFERENCE_ITEM1)
                .addRecord()
                .fillInputText("Имя получателя:", QUARANTINE_RECIPIENT)
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", TRUSTED_RECIPIENT)
                .save();
    }

    @Test(
            description = " Отправить транзакцию №1 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №1",
            dependsOnMethods = "addClient"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE);
    }

    @Test(
            description = "Отправить транзакцию №2 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №2",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(250))
                .withTSPName(TRUSTED_RECIPIENT)
                .withTSPType(TRUSTED_RECIPIENT);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, IN_WHITE_LIST);
    }

    @Test(
            description = "Отправить транзакцию №3 от Клиента №1 \"Платеж по QR-коду через СБП\" -- В транзакции нет Наименования ТСП",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(20))
                .withTSPName(null)
                .withTSPType(null);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "В транзакции отсутствует Наименование ТСП. Невозможно идентифицировать получателя");
    }

    @Test(
            description = "Отправить транзакцию №4 от Клиента №1 \"Платеж по QR-коду через СБП\" -- Получатель №3",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200))
                .withTSPName("Петр Иванович Калашников")
                .withTSPType("Петр Иванович Калашников");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, ADD_TO_QUARANTINE_LIST);
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step4"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(200))
                .withTSPName(QUARANTINE_RECIPIENT)
                .withTSPType(QUARANTINE_RECIPIENT);
        return transaction;
    }

}
