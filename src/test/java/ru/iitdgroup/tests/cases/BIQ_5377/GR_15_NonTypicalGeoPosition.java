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

//TODO: Перед запуском теста должны быть установлены справочники GIS

public class GR_15_NonTypicalGeoPosition extends RSHBCaseTest {

    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.DECEMBER, 23, 14, 10, 0);
    private final List<String> clientIds = new ArrayList<>();
    private String[][] names = {{"Борис", "Кудрявцев", "Викторович"}};

    private static final String RULE_NAME = "R01_GR_15_NonTypicalGeoPosition";
    private static final String REFERENCE_ITEM1 = "(System_parameters) Интеграционные параметры";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Параметры обработки справочников и флагов";

    private static final String TSP_TYPE = "Предприниматель";


    @Test(
            description = "Создаем клиента"
    )
    public void addClient() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");

                client.getData().getClientData().getClient()
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withDboId(dboId);

                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Включить правило R01_GR_15_NonTypicalGeoPosition",
            dependsOnMethods = "addClient"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);
    }

    @Test(
            description = "Включить интеграцию с ГИС справочник \"(System_parameters) Интеграционные параметры\"" +
                    "GisSystem_GIS = 1 и В справочнике \"Параметры обработки справочников и флагов\"" +
                    "TIME_AFTER_ADDING_TO_QUARANTINE = 1",
            dependsOnMethods = "enableRules"
    )

    public void addRecipients() {

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Код значения", "GisSystem_GIS")
                .edit().fillInputText("Значение:", "1")
                .save();

        getIC().locateTable(REFERENCE_ITEM2)
                .findRowsBy()
                .match("код значения", "TIME_AFTER_ADDING_TO_QUARANTINE")
                .edit().fillInputText("Значение:", "1")
                .save();
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию №1 от клиента №1 \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "addRecipients"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_ADD_QUARATINE_LOCATION);
    }

    @Test(
            description = "Провести транзакцию №2 от клиента №1 \"Платеж по QR-коду через СБП\"",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, YOUNG_QUARANTINE_LOCATION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }

}
