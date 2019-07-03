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
    public void editReferenceData() {
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
    public void editReferenceDataOrganizationType() {
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
    public void editReferenceDataPayeeInBlackList() {
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_BLACK_PAYEE_CARD_ACCOUNT).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_BLACK_PAYEE_CARD_ACCOUNT)
                .addRecord()
                .fillMasked("БИК:", "042301968")
                .fillMasked("Сводный счет:", "23451810100000000004")
                .fillMasked("Счет/карта получателя:", "23451810835620000480")
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
            description = "Провести транзакцию № 1 Перевод на счет на реквизиты получателя из справочника запрещенных",
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
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("23451810835620000480");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301968");
        transactionData
                .getOuterTransfer()
                .setOperationDescription("Оплата кредита по кд 0020726477 Чижикова А.С. сч.23451810100000000004 ТКС Банк(ОАО)");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_PAYEE_ACCOUNT_IS_ON_THE_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 2 на реквизиты получателя, где БИК и СЧЕТ в запрещенных, а Сводный карта/счет - нет",
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
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("23451810835620000480");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301968");
        transactionData
                .getOuterTransfer()
                .setOperationDescription("Оплата кредита по кд 0020726477 Чижикова А.С. сч.23451810100000000505 ТКС Банк(ОАО)");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_PAYEE_ACCOUNT_NOT_ON_THE_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 3 на реквизиты получателя, где БИК и Сводный карта/счет в запрещенных, а СЧЕТ - нет",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("23451810835620000500");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301968");
        transactionData
                .getOuterTransfer()
                .setOperationDescription("Оплата кредита по кд 0020726477 Чижикова А.С. сч.23451810100000000004 ТКС Банк(ОАО)");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_PAYEE_ACCOUNT_NOT_ON_THE_BLACK_LIST);
    }
    @Test(
            description = "Провести транзакцию № 4 на реквизиты получателя, где Сводный карта/счет и СЧЕТ в запрещенных, а БИК - нет",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("23451810835620000480");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301551");
        transactionData
                .getOuterTransfer()
                .setOperationDescription("Оплата кредита по кд 0020726477 Чижикова А.С. сч.23451810100000000004 ТКС Банк(ОАО)");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_PAYEE_ACCOUNT_IS_ON_THE_BLACK_LIST);
    }

    @Test(
            description = "Провести транзакцию № 5 на реквизиты получателя, где БИК, СЧЕТ и Сводный карта/счет не в справочнике запрещенных",
            dependsOnMethods = "step4"
    )

    public void step5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("23451810835620000551");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("042301654");
        transactionData
                .getOuterTransfer()
                .setOperationDescription("Оплата кредита по кд 0020726477 Чижикова А.С. сч.23451810100000000987 ТКС Банк(ОАО)");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_RULE_PAYEE_ACCOUNT_IS_ON_THE_BLACK_LIST);
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
