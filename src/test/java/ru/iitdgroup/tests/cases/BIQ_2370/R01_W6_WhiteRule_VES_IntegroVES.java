package ru.iitdgroup.tests.cases.BIQ_2370;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class R01_W6_WhiteRule_VES_IntegroVES extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_06_VES";


    private final GregorianCalendar time = new GregorianCalendar(2019, Calendar.JULY, 10, 0, 0, 0);
    private final List<String> clientIds = new ArrayList<>();


    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:","2000")
                .save()
                .sleep(5);
    }

    @Test(
            description = "Выключить интеграцию с VES",
            dependsOnMethods = "enableRules"
    )

    public void disableVES(){
        getIC().locateTable("(System_parameters) Интеграционные параметры")
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по суждения . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "0").save();
        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "disableVES"
    )
    public void client() {
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
            description = "Отправить транзакцию 1, любую по Клиенту 1",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));

        sendAndAssert(transaction);
        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(NOT_TRIGGERED, DISABLED_IntegrVES1);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
