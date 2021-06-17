package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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

//TODO перед запуском теста в правило должно быть добавлено поле "Промежуток времени с момента восстановления доступа к ДБО"

    private static final String RULE_NAME = "R01_GR_99_Scenario";
    private static final String TABLE_NAME = "(Policy_parameters) Блоки сценариев";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};

    @Test(
            description = "Включаем правило R01_GR_99_Scenario и выполняем преднастройки"
    )

    public void enableRules() {
        String ID_BLOK1;
        String ID_BLOK2;

        Table table = getIC().locateTable(TABLE_NAME);
        int count = table
                .findRowsBy()
                .match("Transaction Type", "Перевод на счет другому лицу")
                .countMatchedRows();

        if (count == 0) {//если записи отсутствуют, заносим новые
            table.addRecord()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                    .save();
        } else {
            getIC().locateTable(TABLE_NAME)
                    .findRowsBy()
                    .match("Transaction Type", "Перевод на счет другому лицу")
                    .click().edit()
                    .fillInputText("Минимальное количество транзакций в серии:", "1")
                    .fillInputText("Минимальная сумма всех транзакций серии:", "1000")
                    .fillInputText("Минимальная сумма транзакции:", "1000")
                    .fillFromExistingValues("Transaction Type:", "Наименование типа транзакции", "Equals", "Перевод на счет другому лицу")
                    .save();
        }
        ID_BLOK1 = copyThisLine("ID:");

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

        ID_BLOK2 = copyThisLine("ID:");

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Максимальный промежуток времени (Смена данных клиента):", "")
                .fillInputText("Максимальный промежуток времени (смена IMSI):", "")
                .fillInputText("Максимальный промежуток времени (подключение ДБО ФЛ):", "")
                .fillInputText("Промежуток времени с момента восстановления доступа к ДБО:", "30")
                .fillInputText("Логическое выражение:", ID_BLOK1 + "&&" + ID_BLOK2)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(30);

        getIC().close();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
        time.add(Calendar.MINUTE, -30);
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");

                client.getData()
                        .getClientData()
                        .getClient()
                        .withPasswordRecoveryDateTime(time)
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
            description = "Провести транзакцию №1 от клиента - \"Перевод другому лицу\", сумма 1001",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransactionOuterTransfer();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
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
        Transaction transaction = getTransactionPAYMENTC2B_QRCODE();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(1002));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, SCENARIO_BLOCK_TRUE);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionPAYMENTC2B_QRCODE() {
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
        return transaction;
    }

    private Transaction getTransactionOuterTransfer() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        return transaction;
    }

}
