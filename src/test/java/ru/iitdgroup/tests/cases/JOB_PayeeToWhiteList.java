package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class JOB_PayeeToWhiteList extends RSHBCaseTest {


    private static final String TABLE = "(Policy_parameters) Параметры обработки справочников и флагов";
    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String TABLE_2 = "(Rule_tables) Сводные счета";
    private static final String REFERENCE_ITEM_ORGANIZATION_TYPE = "(Rule_tables) Типы организаций";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 1, 1, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
//    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .selectRule(RULE_NAME)
//                .activate();
//
////        getIC().locateTable(TABLE).findRowsBy().match("Код значения:","CLAIM_PERIOD")
////                .click().edit()
////                .fillInputText("Значение:","1");
////        FIXME требуется реализовать работу с данным справочником
//
//    }
//
//    @Test(
//            description = "в \"Сводные счета\" указана маска счета для транзакций 1.5, 1.6, 1.7",
//            dependsOnMethods = "enableRules"
//    )
//    public void editReferenceTable() {
//        Table.Formula rows = getIC().locateTable(TABLE_2).findRowsBy();
//        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
//            rows.delete();
//        }
//        getIC().locateTable(TABLE_2)
//                .addRecord()
//                .fillMasked("Маска счёта:", "12345")
//                .save();
//    }
//
//    @Test(
//            description = "В справочнике Типы организаций заведены значения ООО, ОАО АО",
//            dependsOnMethods = "editReferenceTable"
//    )
//
//    public void editReferenceDataOrganizationType(){
//        Table.Formula rows = getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE).findRowsBy();
//        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
//            rows.delete();
//        }
//        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
//                .addRecord()
//                .fillMasked("Тип организации:", "ОАО")
//                .save();
//        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
//                .addRecord()
//                .fillMasked("Тип организации:", "АО")
//                .save();
//        getIC().locateTable(REFERENCE_ITEM_ORGANIZATION_TYPE)
//                .addRecord()
//                .fillMasked("Тип организации:", "ООО")
//                .save();
//        getIC().close();
//    }
//
//    @Test(
//            description = "Создаем клиента",
//            dependsOnMethods = "editReferenceDataOrganizationType"
//    )
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Провести транзакцию № 1.1 Оплата услуг",
            dependsOnMethods = "step0"
    )
    public void step1() {
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.2  Перевод на карту",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.3. Перевод на счёт (в пользу юридического лица) не на сводный счёт",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        transactionData.getOuterTransfer().getPayeeProps()
                .setPayeeName("ООО Ромашка");
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.4. Перевод на счёт (в пользу физического лица) не на сводный счёт",
            dependsOnMethods = "step3"
    )
    public void step4() {

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeName("Илюшин Дыня Петрович");
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("42304810835620011555");
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.5. Перевод на счёт, сводный, где \"Наименование получателя\" = \"Иванов Иван Иванович\"",
            dependsOnMethods = "step4"
    )
    public void step5() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeName("Иванов Иван Иванович");
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620000481");
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.6. Перевод на счёт, сводный, где ИНН 12 символов",
            dependsOnMethods = "step5"
    )
    public void step6() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeName(null);
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620000480");
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeINN("123456789077");
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.7. Перевод на счет, сводный, где \"Наименование получателя\" = \"банк\" И \"Описание операции\" = \"На расходы 4276-0708 0081 3281\"",
            dependsOnMethods = "step6"
    )
    public void step7() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeName("банк");
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620000485");
        transactionData.getOuterTransfer()
                .setOperationDescription("На расходы 4276-0708 0081 3281");
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.8. Перевод через систему денежных переводов",
            dependsOnMethods = "step7"
    )
    public void step8() {
        Transaction transaction = getTransactionSPD();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        sendAndAssert(transaction);
    }

    @Test(
            description = "Провести транзакцию № 1.9. Изменение перевода через систему денежных переводов (изменение перевода 1.8) с изменением получателя",
            dependsOnMethods = "step8"
    )
    public void step9() {
        Transaction transaction = getTransactionSPD_Refactor();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData.getClientIds().withDboId(clientIds.get(0));
        sendAndAssert(transaction);

//        TODO требуется доделать Тест кейс после доработки по перезагрузки САФ и Игнайт
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionSPD() {
        Transaction transaction = getTransaction("testCases/Templates/SDP.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionSPD_Refactor() {
        Transaction transaction = getTransaction("testCases/Templates/SDP_Refactor.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
