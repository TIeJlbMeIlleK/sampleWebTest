package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BR_01_PayeeInBlackList_2 extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String REFERENCE_ITEM_SVOD_ACCOUNT = "(Rule_tables) Сводные счета";
    private static final String REFERENCE_ITEM_ORGANIZATION_TYPE = "(Rule_tables) Типы организаций";
    private static final String REFERENCE_ITEM_BLACK_PAYEE_CARD_ACCOUNT = "(Rule_tables) Запрещенные получатели Сводный Счет/карта получателя";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )

            public void enableRules() {
            getIC().locateRules()
                    .selectVisible()
                    .deactivate()
                    .selectRule(RULE_NAME)
                    .activate()
                    .sleep(3);
            }

            @Test(
            description = "В справочнике Сводные счета заводим маску сводного счета",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_SVOD_ACCOUNT).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_SVOD_ACCOUNT)
                .addRecord()
                .fillMasked("Маска счёта:", "12345")
                .save();

    }
    @Test(
            description = "Добавляем типы организаций",
            dependsOnMethods = "editReferenceData"
    )

    public void editReferenceDataOrganizationType(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
                .addRecord()
                .fillMasked("Тип организации:", "ОАО")
                .save();
        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
                .addRecord()
                .fillMasked("Тип организации:", "АО")
                .save();
        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
                .addRecord()
                .fillMasked("Тип организации:", "ООО")
                .save();

    }
    @Test(
            description = "В справочник Запрещенные получатели сводный счет/карта занесем значения БИК, СЧЕТ, Сводный карта/Счет",
            dependsOnMethods = "editReferenceDataOrganizationType"
    )
    public void editReferenceDataPayeeInBlackList(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_BLACK_PAYEE_CARD_ACCOUNT).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_BLACK_PAYEE_CARD_ACCOUNT)
                .addRecord()
                .fillMasked("БИК:", "042301968")
                .save();
        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
                .addRecord()
                .fillMasked("Сводный счет:", "12345810100000000004")
                .save();
        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
                .addRecord()
                .fillMasked("Счет/карта получателя:", "12345810835620000480")
                .save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceDataPayeeInBlackList"
    )
    public void step0() {
        try {
            for (int i = 0; i < 1; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient()
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
            description = "Провести транзакцию № 1 \"Перевод на счет\" на реквизиты получателя из справочника запрещенных",
            dependsOnMethods = "step0"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getCardTransfer()
                .setDestinationCardNumber("4378723741117915");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_CARD_IN_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 1 Перевод на карту на карту из справочника запрещенных",
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
                .getCardTransfer()
                .setDestinationCardNumber("1234523741117915");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED,RESULT_RULE_CARD_NOT_IN_BLACK_LIST);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
