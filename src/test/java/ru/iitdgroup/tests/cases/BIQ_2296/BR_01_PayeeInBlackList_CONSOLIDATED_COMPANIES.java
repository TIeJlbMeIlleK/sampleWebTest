package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class BR_01_PayeeInBlackList_CONSOLIDATED_COMPANIES extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_BR_01_PayeeInBlackList";
    private static final String REFERENCE_ITEM_CONSOLIDATED_MASK = "(Rule_tables) Сводные счета";
    private static final String REFERENCE_ITEM_ORGANIZATION_TYPE = "(Rule_tables) Типы организаций";
    private static final String REFERENCE_ITEM_BLACK_CONSOLIDATED_FIO = "(Rule_tables) Запрещенные получатели Сводный ФИО получателя";

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
            description = "Запрещенные получатели Сводный ФИО получателя занести значения БИК, СЧЕТ, Сводный ФИО получателя",
            dependsOnMethods = "enableRules"
    )

    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_BLACK_CONSOLIDATED_FIO).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_BLACK_CONSOLIDATED_FIO)
                .addRecord()
                .fillMasked("ФИО получателя:", "ООО Рога и Копыта")
                .fillMasked("Сводный счет:","12345810835620011555")
                .fillMasked("БИК:","044805111")
                .save();
    }
    @Test(
            description = "Занести максу счета в справочник Сводные счета",
            dependsOnMethods = "editReferenceData"
    )

    public void editReferenceDataConsolidatedMask(){
        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_CONSOLIDATED_MASK).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateTable(REFERENCE_ITEM_CONSOLIDATED_MASK)
                .addRecord()
                .fillMasked("Маска счёта:", "12345")
                .save();

    }
    @Test(
            description = "В справочнике Типы организаций заведены значения ООО, ОАО АО",
            dependsOnMethods = "editReferenceDataConsolidatedMask"
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
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "editReferenceDataOrganizationType"
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
                .setOperationDescription("ООО Рога и Копыта");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011555");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805111");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
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
