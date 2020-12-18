package ru.iitdgroup.tests.cases.BIQ_4077;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.commons.lang3.RandomStringUtils;
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

public class GR_26_GrayPassportHash extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_26_DocumentHashInGrayList";
    private static String  hash = null;
    private static final String  TABLE = "(Rule_tables) Подозрительные документы клиентов";

    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();
    private final List<String> clientIds1 = new ArrayList<>();


    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(30);
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 2; i++) {
                //FIXME Добавить проверку на существование клиента в базе
                String dboId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "";
                Client client = new Client("testCases/Templates/client.xml");
                client
                        .getData()
                        .getClientData()
                        .getClient().withLogin(dboId)
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                client.getData().getClientData().getClientDocument().get(0).setDocType("21");
                client.getData().getClientData().getClientDocument().get(0).setSeries(RandomStringUtils.randomNumeric(4));
                client.getData().getClientData().getClientDocument().get(0).setNumber(RandomStringUtils.randomNumeric(6));
                sendAndAssert(client);
                clientIds.add(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "Занести hash действующего клиента в справочник подозрительных",
            dependsOnMethods = "client"
    )
    public void  putOnGreyList(){
        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().locateReports().openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента","Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        hash = copyThisLine("Hash действующего документа:");

//        hash = driver.findElementByXPath("//span[text()='Идентификатор клиента:']/../following::td").getText();


        getIC().locateTable(TABLE)
                .addRecord()
                .fillInputText("Hash документа:",hash)
                .select("Причина занесения:","Первичная загрузка")
                .save();
    }

    @Test(
            description = "Отправить любую транзакцию № 1 от клиента 1",
            dependsOnMethods = "putOnGreyList"
    )
    public void transaction1() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, RESULT_GRAY_LIST);
    }

    @Test(
            description = "Отправить любую транзакцию № 2 от клиента 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES_FOUND);

        Table.Formula rows = getIC().locateTable(TABLE).findRowsBy();
        if (rows.calcMatchedRows().getTableRowNums().size() > 0) {
            rows.delete();
        }
        getIC().close();
    }

    @Test(
            description = "Отправить любую транзакцию № 3 от клиента 1",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        Transaction transaction = getTransactionGETTING_CREDIT();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, NO_MATCHES_FOUND);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionGETTING_CREDIT() {
        Transaction transaction = getTransaction("testCases/Templates/GETTING_CREDIT_PC.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
