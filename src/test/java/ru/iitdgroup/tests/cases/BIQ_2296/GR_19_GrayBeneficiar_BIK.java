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

public class GR_19_GrayBeneficiar_BIK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_19_GrayBeneficiar";
    private String GRAY_BIK = "042301970";
    private String WHITE_BIK = "042301501";
    private String GRAY_ACCOUNT = "12345810835620000354";
    private String WHITE_ACCOUNT = "12345810835620000555";

    private static final String  TABLE = "(Rule_tables) Подозрительные получатели БИКСЧЕТ";




    private final GregorianCalendar time = new GregorianCalendar(2020, Calendar.JANUARY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Генерация клиентов"
    )
    public void client() {
        System.out.println("Правило GR_19 работает с \"Подозрительные получатели БИКСЧЕТ\" ТК№ 33 BIQ2296");
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
            description = "Настройка и включение правила",
            dependsOnMethods = "client"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(15);
    }

    @Test(
            description = "Настройка и включение правила",
            dependsOnMethods = "enableRules"
    )
    public void editReferenceData(){
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }

        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("БИК:",GRAY_BIK)
                .fillInputText("Счет:",GRAY_ACCOUNT)
                .save();
        getIC().close();

    }

    @Test(
            description = "Провести транзакцию № 1 \"Перевод на счет\" на БИК+СЧЕТ из справочника подозрительных",
            dependsOnMethods = "editReferenceData"
    )
    public void step1() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps().setPayeeAccount(GRAY_ACCOUNT);
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(GRAY_BIK);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAY_BENEFICIAR_BIC_ACC);
    }
    @Test(
            description = "Провести транзакцию № 2 \"Перевод на счет\" на БИК из справочника подозрительных, а СЧЕТ - нет.",
            dependsOnMethods = "step1"
    )
    public void step2() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps().setPayeeAccount(WHITE_ACCOUNT);
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(GRAY_BIK);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }

    @Test(
            description = "Провести транзакцию № 3 \"Перевод на счет\" на СЧЕТ из справочника подозрительных, а БИК - нет.",
            dependsOnMethods = "step2"
    )
    public void step3() {
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getOuterTransfer()
                .getPayeeProps().setPayeeAccount(GRAY_ACCOUNT);
        transactionData
                .getOuterTransfer()
                .getPayeeBankProps()
                .setBIK(WHITE_BIK);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }


    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
