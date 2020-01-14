package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_05_TransferOnOldAcc_TRIGGERED extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_05_TransferOnOldAcc";

    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "Настройка и включение правила"
    )
    public void enableRules() {

        System.out.println("Правило WR_05 не срабатывает для непроверяемых типов транзакций и несоблюдении условий сработки" + "ТК№23 --- BIQ2296");
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .sleep(3);

        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Период в днях для «старого» счёта:","1")
                .save()
                .sleep(5);

        Table.Formula rows1 = getIC().locateTable("(Rule_tables) БИК Банка").findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }

        getIC().locateTable("(Rule_tables) БИК Банка")
                .addRecord()
                .fillInputText("БИК:","044525219")
                .fillInputText("Регион:","Москва")
                .save();

        getIC().close();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    @Test(
            description = "Провести транзакцию \"Перевод на счет\" (БИК в справочнике \"БИК Банка\", \"Дата открытия счета получателем\" 1 дней назад)",
            dependsOnMethods = "step0"
    )

    public void step1() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy +3:00");
        LocalDateTime localDateTimeMinus9Days = LocalDateTime.now().minusDays(1);
        dateTimeFormatter.format(localDateTimeMinus9Days);
        GregorianCalendar calendar = GregorianCalendar.from(localDateTimeMinus9Days.atZone(ZoneId.systemDefault()));
        XMLGregorianCalendar xmlGregorianCalendar = new XMLGregorianCalendarImpl(calendar);

        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getOuterTransfer()
                .getPayeeBankProps().setBIK("044525219");
        transactionData.getOuterTransfer()
                .getPayeeProps()
                .setPayeeAccountOpenDate(xmlGregorianCalendar);

        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(TRIGGERED, EXIST_OLD_ACCOUNT);
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
