package ru.iitdgroup.tests.cases.BIQ_2296;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.client.PCDevice;
import ru.iitdgroup.intellinx.dbo.client.PlatformKind;
import ru.iitdgroup.intellinx.dbo.transaction.ChannelType;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.ves.mock.VesMock;
import ru.iitdgroup.tests.webdriver.referencetable.Table;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_09_UseNewMobileDevice extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_ExR_09_UseNewMobileDevice";
    private static final String TABLE= "(System_parameters) Интеграционные параметры";



    private final GregorianCalendar time = new GregorianCalendar(Calendar.getInstance().getTimeZone());
    private final List<String> clientIds = new ArrayList<>();
//TODO требуется реализовать отправку сообщения через новый ВЭС

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
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", false)
                .save()
                .sleep(5);

    }
    @Test(
            description = "Выключить IntegrVES2",
            dependsOnMethods = "enableRules"
    )
    public void disableVES() {

        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "0").save();
    }
    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "disableVES"
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
            description = "Провести транзакцию № 1 из Мобильного банка",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        Table.Formula rows1 = getIC().locateTable("(Rule_tables) Доверенные устройства для клиента").findRowsBy();
        if (rows1.calcMatchedRows().getTableRowNums().size() > 0) {
            rows1.delete();
        }
        getIC().locateTable("(Rule_tables) Доверенные устройства для клиента")
                .addRecord()
                .fillUser("Клиент:",clientIds.get(0))
                .fillInputText("IMEI:", "1")
                .fillInputText("IdentifierForVendor:","1")
                .fillInputText("IMSI:","1")
                .fillCheckBox("Доверенный:", true)
                .save();

        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        sendAndAssert(transaction);
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, DISABLED_INTEGR_VES_1);
    }

    @Test(
            description = "Включить IntegrVES2",
            dependsOnMethods = "transaction1"
    )
    public void enableVES() {
        getIC().locateTable(TABLE)
                .findRowsBy()
                .match("Description", "Интеграция с ВЭС по необработанным данным . Если параметр включен – интеграция производится.")
                .click()
                .edit()
                .fillInputText("Значение:", "1").save();
    }

    @Test(
            description = "В настройке правила ВЫКЛючить флаги Использовать информацию из ВЭС и Использовать информацию из САФ",
            dependsOnMethods = "enableVES"
    )
    public void disableCheckboxesInRule() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Использовать информацию из ВЭС:",false)
                .fillCheckBox("Использовать информацию из САФ:", false)
                .save()
                .sleep(15);
    }

    @Test(
            description = "Провести траназкцию № 2 из Мобильного банка",
            dependsOnMethods = "disableCheckboxesInRule"
    )
    public void transaction2() {
//TODO требуется реализовать отправку сообщения через новый ВЭС

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Transaction transaction = getTransaction();
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
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);
    }
    @Test(
            description = "В настройке правила ВКЛючить флаг Использовать информацию из САФ",
            dependsOnMethods = "transaction2"
    )
    public void enableCheckbox1() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(15);
    }
    @Test(
            description = "Провести транзакцию № 3 из Мобильного банка iOS, в контейнере устройства не присылать IFV",
            dependsOnMethods = "enableCheckbox1"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI(null);
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("1567156156741");
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, NO_IMEI_EXR9);
    }

    @Test(
            description = "Провести транзакцию № 4 из Мобильного банка Android, в контейнере устройства не присылать IMEI",
            dependsOnMethods = "transaction3"
    )
    public void transaction4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("15671561165721327");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI(null);
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, NO_IMSI_EXR9);
    }

    @Test(
            description = "Провести транзакцию № 4 из Мобильного банка Android, в контейнере устройства не присылать IMSI",
            dependsOnMethods = "transaction4"
    )

    public void enableAllCheckboxesInRule() {
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Использовать информацию из ВЭС:",true)
                .fillCheckBox("Использовать информацию из САФ:", true)
                .save()
                .sleep(15);
        getIC().close();
    }

    @Test(
            description = "Провести транзакцию № 5 из Мобильного банка",
            dependsOnMethods = "enableAllCheckboxesInRule"
    )
    public void transaction5() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(1));
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMEI("1567156156741");
        transactionData
                .getClientDevice()
                .getAndroid()
                .setIMSI("1567156156741");
        sendAndAssert(transaction);
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertLastTransactionRuleApply(FEW_DATA, DEVICE_NOT_EXIST);
    }
    @Test(
            description = "Провести транзакцию № 6 из Интернет банка",
            dependsOnMethods = "transaction5"
    )
    public void transaction6() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(false);
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData.getClientDevice().setAndroid(null);
        transactionData.getClientDevice().setPC(new PCDevice());
        transactionData.getClientDevice()
                .getPC()
                .setIpAddress("123.22.57.8");
        transactionData.getClientDevice()
                .getPC()
                .setBrowserData(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.getClientDevice()
                .getPC()
                .setUserAgent(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");
        transactionData.setChannel(ChannelType.INTERNET_CLIENT);
        transactionData.getClientDevice().setPlatform(PlatformKind.PC);
        transactionData.setSessionId(ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "");

        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, INTERNET_BANK_TRANSACTION);
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER_MOBILE.xml");
        transaction.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        return transaction;
    }
}
