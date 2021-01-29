package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class GR_99_Scenario extends RSHBCaseTest {

//TODO перед  запуском теста должно быть вставлено поле в правило "Промежуток времени с момента восстановления доступа к ДБО"

    private static final String RULE_NAME = "R01_GR_99_Scenario";
    private static String TABLE_NAME = "(Policy_parameters) Блоки сценариев";

    private final GregorianCalendar time = new GregorianCalendar(2021, Calendar.JANUARY, 25, 0, 0, 0);

    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private static final String LOGIN = new RandomString(5).nextString();
    private static final String LOGIN_HASH = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 5);

    @Test(
            description = "Включаем правило R01_GR_99_Scenario и выполняем преднастройки"
    )
//TODO Логическое выражение в правиле должно совпадать с ID Блоками сценария
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:", "20")
                .fillInputText("Логическое выражение:", "9&&10")
                .save()
                .sleep(10);

        Table table = getIC().locateTable(TABLE_NAME);
        int count = table
                .findRowsBy()
                .match("Transaction Type", "Перевод другому лицу")
                .countMatchedRows();

        if (count == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод другому лицу")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Перевод другому лицу")
                    .click().edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод другому лицу")
                    .save();
        }
        int count1 = table
                .findRowsBy()
                .match("Transaction Type", "Платеж по QR-коду через СБП")
                .countMatchedRows();

        if (count1 == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Платеж по QR-коду через СБП")
                    .click().edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                    .save();
        }
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
                        .withLogin(LOGIN)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(LOGIN_HASH)
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
            description = "Провести транзакцию №1 от клиента - \"Перевод другому лицу\", сумма 1001",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1002));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию №2 от клиента - \"Платеж по QR-коду через СБП\", сумма 1001",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction2 = getTransactionPAYMENTC2B_QRCODE();
        TransactionDataType transactionData2 = transaction2.getData().getTransactionData()
                .withRegular(false);
        transactionData2
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData2
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1002));
        sendAndAssert(transaction2);
        assertLastTransactionRuleApply(TRIGGERED, SCENARIO_BLOCK_TRUE);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPAYMENTC2B_QRCODE() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
