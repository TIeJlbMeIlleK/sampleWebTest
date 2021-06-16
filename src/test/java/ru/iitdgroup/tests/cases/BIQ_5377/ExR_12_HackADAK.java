package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;
import ru.iitdgroup.tests.webdriver.alerts.AlertRecord;
import ru.iitdgroup.tests.mock.commandservice.CommandServiceMock;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExR_12_HackADAK extends RSHBCaseTest {

    public CommandServiceMock commandServiceMock = new CommandServiceMock(3005);
    private static final String RULE_NAME = "R01_ExR_12_HackADAK";
    private static final String RULE_NAME1 = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Вопросы для проведения ДАК";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры проведения ДАК";
    public String TRANSACTION_ID;
    public final String IP_ADRESS = "178.219.186.12";
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();
    public final String nameClient = "Лариса";
    public final String notNameClient = "Зинаида";
    private final String[][] names = {{nameClient, "Касымова", "Игоревна"}};
    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП

//TODO перед запуском теста В Details Layout в карточке клиента должны быть выведены поля "Подбор ответа АДАК" и "Дата подбора АДАК"
    // TODO должен быть включен только один вопрос в справочнике Вопросы ДАК: Ваше имя. Остальные вопросы выключены

    @Test(
            description = "Включаем правило и настраиваем справочники"
    )

    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Статусы АДАК:", "WRONG, REFUSE")
                .fillInputText("Период серии (в минутах):", "10")
                .fillInputText("Количество неуспешных попыток :", "2")
                .save();
        getIC().GoToTheListRule()
                .selectVisible()
                .editRule(RULE_NAME1)
                .fillCheckBox("Active:", true)
                .save()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM1)
                .findRowsBy()
                .match("Описание", "Таймаут на получение ответа АДАК")
                .click()
                .edit()
                .fillInputText("Значение:", "200000")
                .save();

        getIC().locateTable(REFERENCE_ITEM)
                .findRowsBy()
                .match("Текст вопроса клиенту", "Ваше имя")
                .click()
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true)
                .save();
        commandServiceMock.run();
    }

    @Test(
            description = "Создание клиентов",
            dependsOnMethods = "enableRules"
    )
    public void createClients() {
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
            description = "Провести транзакцию № 1 от клиента № 1 \"Платеж по QR-коду через СБП\", на вопрос АДАК ответить верно (SUCCESS)",
            dependsOnMethods = "createClients"
    )

    public void step1() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);

        Transaction adak = getAdak();
        TransactionDataType tranAdak = adak.getData().getTransactionData();
        tranAdak
                .withTransactionId(TRANSACTION_ID);
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Транзакция:", TRANSACTION_ID);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "SUCCESS");
    }

    @Test(
            description = "Провести транзакцию № 2 от клиента № 1 \"Платеж по QR-коду через СБП\", на вопрос АДАК ответить неверно (WRONG)",
            dependsOnMethods = "step1"
    )

    public void step2() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);

        Transaction adak = getAdak();
        TransactionDataType tranAdak = adak.getData().getTransactionData();
        tranAdak
                .withTransactionId(TRANSACTION_ID);
        tranAdak.getAdditionalAnswer()
                .withAdditionalAuthAnswer(notNameClient);
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Транзакция:", TRANSACTION_ID);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "WRONG");
    }

    @Test(
            description = "Провести транзакцию № 3 от клиента № 1 \"Платеж по QR-коду через СБП\", на вопрос АДАК ответить неверно (WRONG)",
            dependsOnMethods = "step2"
    )

    public void step3() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, RESULT_RULE_NOT_APPLY);

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);

        Transaction adak = getAdak();
        TransactionDataType tranAdak = adak.getData().getTransactionData();
        tranAdak
                .withTransactionId(TRANSACTION_ID);
        tranAdak.getAdditionalAnswer()
                .withAdditionalAuthAnswer(notNameClient);
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        assertTableField("Транзакция:", TRANSACTION_ID);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "WRONG");

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        assertTableField("Подбор ответа на АДАК:", "No");
        assertTableField("Дата подбора АДАК:", "");
    }

    @Test(
            description = "Провести транзакцию № 4 от клиента № 1 \"Платеж по QR-коду через СБП\", на вопрос АДАК ответить неверно (WRONG)",
            dependsOnMethods = "step3"
    )

    public void step4() {
        Transaction transaction = getTransaction();
        TransactionDataType transactionData = transaction.getData().getTransactionData();
        TRANSACTION_ID = transactionData.getTransactionId();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "Попытка подбора ответа на АДАК");

        getIC().locateAlerts()
                .openFirst()
                .action("Выполнить АДАК")
                .sleep(1);

        time.add(Calendar.SECOND, 1);
        Transaction adak = getAdak();
        TransactionDataType tranAdak = adak.getData().getTransactionData();
        tranAdak
                .withTransactionId(TRANSACTION_ID);
        tranAdak.getAdditionalAnswer()
                .withAdditionalAuthAnswer(notNameClient);
        sendAndAssert(adak);

        getIC().locateAlerts()
                .openFirst();
        String lastDate = copyThisLine("Timestamp:");

        assertTableField("Транзакция:", TRANSACTION_ID);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Статус АДАК:", "WRONG");

        getIC().locateReports()
                .openFolder("Бизнес-сущности")
                .openRecord("Список клиентов")
                .setTableFilterWithActive("Идентификатор клиента", "Equals", clientIds.get(0))
                .runReport()
                .openFirst();
        assertTableField("Подбор ответа на АДАК:", "Yes");
        assertTableField("Дата подбора АДАК:", lastDate);// нужно взять дату с Алерта или из БД
        getIC().close();
    }

    @Test(
            description = "Выключить мок ДБО",
            dependsOnMethods = "step4"
    )

    public void disableCommandServiceMock() {
        commandServiceMock.stop();
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransaction() {
        Transaction transaction = getTransaction("testCases/Templates/PAYMENTC2B_QRCODE.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionData = transaction.getData().getTransactionData()
                .withVersion(1L)
                .withRegular(false)
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionData
                .getClientIds()
                .withDboId(clientIds.get(0));
        transactionData
                .getPaymentC2B()
                .withAmountInSourceCurrency(BigDecimal.valueOf(300))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        transactionData
                .getClientDevice()
                .getAndroid()
                .withIpAddress(IP_ADRESS);
        return transaction;
    }

    private Transaction getAdak() {
        Transaction adak = getTransaction("testCases/Templates/ADAK.xml");
        adak.getData().getServerInfo().withPort(8050);
        TransactionDataType adakDate = adak.getData().getTransactionData()
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        adakDate
                .getClientIds()
                .withDboId(clientIds.get(0));
        adakDate.getAdditionalAnswer()
                .withAdditionalAuthAnswer(nameClient);
        return adak;
    }
}
