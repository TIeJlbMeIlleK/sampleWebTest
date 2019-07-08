package ru.iitdgroup.tests.cases;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.webdriver.ic.IC;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class ExR_01_AuthenticationContactChanged extends RSHBCaseTest {
    /// Доделать
    private static final String RULE_NAME = "R01_ExR_01_AuthenticationContactChanged";
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

    public void editClient(){
        // требуется реализовать функцию хождения по сущности Клиента Report -- Report management -- Бизнес сущности -- Список клиентов

    }
    @Test(
            description = "Провести люблю транзакцию № 1 от имени клиента № 1, у которого не взведены флаги Смена учетных данных и Изменен IMSI телефона для аутентификации",
            dependsOnMethods = "editClient"
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
                .setOperationDescription("пополнение карты 2200021030605190 Иванов Иван Иванович");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011555");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805111");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_BLOCK_CONSOLIDATED_NAME);
    }

    @Test(
            description = "Провести транзакцию № 2 на реквизиты получателя, где БИК и СЧЕТ в запрещенных, а Сводный ФИО получателя - нет",
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
                .setOperationDescription("пополнение карты 2200021030605190 Петров Дмитрий Сергеевич");
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
    @Test(
            description = "Провести транзакцию № 3 на реквизиты получателя, где БИК и Сводный ФИО получателя в запрещенных, а СЧЕТ - нет",
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
                .setOperationDescription("пополнение карты 2200021030605190 Иванов Иван Иванович");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011678");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805111");
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NOT_EXIST_IN_BLACK_LIST);
    }
    @Test(
            description = "Провести транзакцию № 4 на реквизиты получателя, где Сводный ФИО получателя и СЧЕТ в запрещенных, а БИК - нет",
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
                .setOperationDescription("пополнение карты 2200021030605190 Иванов Иван Иванович");
        transactionData
                .getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccount("12345810835620011555");
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK("044805555");
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
