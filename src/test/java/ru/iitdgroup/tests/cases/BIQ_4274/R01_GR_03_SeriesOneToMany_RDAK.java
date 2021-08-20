package ru.iitdgroup.tests.cases.BIQ_4274;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class R01_GR_03_SeriesOneToMany_RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_03_SeriesOneToMany";
    private static final String RULE_NAME_2 = "R01_ExR_05_GrayIP";
    private static final String IP_ADRESS = "192.168.5.1";
    private static final String TABLE_GREY_IP = "(Rule_tables) Подозрительные IP адреса";
    private static final String TABLE_PARAMETRES = "(Policy_parameters) Параметры обработки событий";
    private static final String TABLE_INTEGRO = "(System_parameters) Интеграционные параметры";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};

    @Test(
            description = "Настройка и включение правил"
    )
    public void enableRules() {
        System.out.println("BIQ2370 и ТК№13(103) - 'Правило GR_03 не учитывает в работе транзакции, подтвержденные по РДАК");

//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .selectRule(RULE_NAME_2)
//                .activate()
//                .editRule(RULE_NAME)
//                .fillCheckBox("Active:", true)
//                .fillInputText("Период серии в минутах:", "10")
//                .fillInputText("Длина серии:", "3")
//                .fillInputText("Сумма серии:", "1000")
//                .fillCheckBox("Проверка регулярных:", true)
//                .save()
//                .sleep(15);
//
//        getIC().locateTable(TABLE_GREY_IP)
//                .addRecord()
//                .fillInputText("IP устройства:", IP_ADRESS)
//                .save();
//        getIC().locateTable(TABLE_PARAMETRES)
//                .deleteAll()
//                .addRecord()
//                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
//                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Перевод на карту другому лицу")
//                .fillCheckBox("Требуется выполнение АДАК:", false)
//                .fillCheckBox("Требуется выполнение РДАК:", true)
//                .fillCheckBox("Учитывать маску правила:", false)
//                .select("Наименование канала ДБО:", "Интернет клиент")
//                .save();
//        getIC().locateTable(TABLE_INTEGRO)
//                .findRowsBy()
//                .match("Код значения", "INCREASED_LOAD")
//                .edit()
//                .fillInputText("Значение:", "0")
//                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void client() {
        try {
            for (int i = 0; i < 1; i++) {
                String dboId = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 7);
                Client client = new Client("testCases/Templates/client.xml");
                client.getData()
                        .getClientData()
                        .getClient()
                        .withLogin(dboId)
                        .withFirstName(names[i][0])
                        .withLastName(names[i][1])
                        .withMiddleName(names[i][2])
                        .getClientIds()
                        .withLoginHash(dboId)
                        .withDboId(dboId)
                        .withCifId(dboId)
                        .withExpertSystemId(dboId)
                        .withEksId(dboId)
                        .getAlfaIds()
                        .withAlfaId(dboId);
                sendAndAssert(client);
                clientIds.add(dboId);
                System.out.println(dboId);
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test(
            description = "2. Провести транзакцию № 1 'Перевод на карту' для клиента № 1, сумма 999" +
                    " Подтвердить транзакцию № 1 по РДАК",
            dependsOnMethods = "client"
    )
    public void transaction1() {
        time.add(Calendar.MINUTE, -30);
        Transaction transaction = getTransactionCARD_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getCardTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(999));
        transactionData
                .getClientDevice()
                .getPC()
                .withIpAddress(IP_ADRESS);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .sleep(2)
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .MyPayment();
    }

    @Test(
            description = "Провести транзакцию № 2 'Перевод по номеру телефона' для клиента № 1, сумма 500, регулярная",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionPHONE_NUMBER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withRegular(true);
        transactionData
                .getPhoneNumberTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(500));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Test(
            description = "Провести транзакцию № 3 'Перевод на счет' для клиента № 1, сумма 400",
            dependsOnMethods = "transaction2"
    )
    public void transaction3() {
        time.add(Calendar.MINUTE, 1);
        Transaction transaction = getTransactionOUTER_TRANSFER();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        transactionData
                .getOuterTransfer()
                .withAmountInSourceCurrency(BigDecimal.valueOf(400));
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Правило не применилось (проверка по настройкам правила)");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransactionOUTER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/OUTER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionCARD_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/CARD_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }

    private Transaction getTransactionPHONE_NUMBER_TRANSFER() {
        Transaction transaction = getTransaction("testCases/Templates/PHONE_NUMBER_TRANSFER.xml");
        transaction.getData().getServerInfo().withPort(8050);
        transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transaction.getData().getTransactionData()
                .getClientIds().withDboId(clientIds.get(0));
        return transaction;
    }
}
