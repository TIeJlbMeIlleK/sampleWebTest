package ru.iitdgroup.tests.cases.BIQ_5377;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.bytebuddy.utility.RandomString;
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

public class Performances_RDAK extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_GR_20_NewPayee";
    private static final String REFERENCE_ITEM = "(Policy_parameters) Проверяемые Типы транзакции и Каналы ДБО";
    private static final String REFERENCE_ITEM1 = "(Policy_parameters) Параметры обработки событий";
    private static final String REFERENCE_ITEM2 = "(Policy_parameters) Вопросы для проведения ДАК";
    private final String[][] names = {{"Ольга", "Петушкова", "Ильинична"}};
    private static final String TSP_TYPE = new RandomString(7).nextString();// создает рандомное значение Типа ТСП
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

//TODO для прохождения теста в Alert должны быть внесены поля: Идентификатор клиента, Status (Алерта), Статус РДАК, status(транзакции)
//TODO В справочнике "Перечень статусов для которых применять РДАК" должны быть прописаны статусы:
// из rdak_underfire в RDAK_Done и из "Wait_RDAK" в "RDAK_Done"

    @Test(
            description = "Занести транзакция в проверяемые, " +
                    "в справочнике событий выбрать клиентов по умолчанию и применить РДАК. " +
                    "Выбрать правило GR_20." +
                    "Заполнить Вопросы для проведения ДАК: " +
                    "registrationHouse, registrationStreet, birthDate с установленными флагами Включено и Учавствует в РДАК"
    )
    public void enableRules() {

        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME)
                .activate()
                .sleep(10);

        getIC().locateTable(REFERENCE_ITEM)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .select("Наименование канала:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM1)
                .deleteAll()
                .addRecord()
                .fillFromExistingValues("Наименование группы клиентов:", "Имя группы", "Equals", "Группа по умолчанию")
                .fillFromExistingValues("Тип транзакции:", "Наименование типа транзакции", "Equals", "Платеж по QR-коду через СБП")
                .fillCheckBox("Требуется выполнение АДАК:", true)
                .fillCheckBox("Требуется выполнение РДАК:", true)
                .select("Наименование канала ДБО:", "Мобильный банк")
                .save();
        getIC().locateTable(REFERENCE_ITEM2)
                .setTableFilter("Текст вопроса клиенту", "Equals", "Ваше имя")
                .refreshTable()
                .click(2)
                .edit()
                .fillCheckBox("Включено:", true)
                .fillCheckBox("Участвует в АДАК:", true)
                .fillCheckBox("Участвует в РДАК:", true)
                .save();
    }

    @Test(
            description = "Создаем клиента",
            dependsOnMethods = "enableRules"
    )
    public void addClient() {
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
            description = "Провести транзакции № 1",
            dependsOnMethods = "addClient"
    )
    public void transaction1() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .notMyPayment()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "WRONG");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 2",
            dependsOnMethods = "transaction1"
    )
    public void transaction2() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .clientNotConfirmed()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "NOT_CONFIRMED_CLIENT");
        assertTableField("status:", "Подозрительная");
    }

    @Test(
            description = "Провести транзакции № 3",
            dependsOnMethods = "transaction1"
    )
    public void transaction3() {
        Transaction transaction = getTransaction();
        sendAndAssert(transaction);

        getIC().locateAlerts()
                .openFirst()
                .action("Взять в работу для выполнения РДАК")
                .rdak()
                .theClientWillCallHimself()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "На выполнении РДАК");
        assertTableField("Статус РДАК:", "CLIENT_CALL");
        assertTableField("status:", "Подозрительная");

        getIC().locateAlerts()
                .openFirst()
                .rdak()
                .fillCheckBox("Верный ответ", true)
                .NotKnown()
                .sleep(1);
        assertTableField("Идентификатор клиента:", clientIds.get(0));
        assertTableField("Status:", "РДАК выполнен");
        assertTableField("Статус РДАК:", "UNKNOWN");
        assertTableField("status:", "Подозрительная");
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
                .withAmountInSourceCurrency(BigDecimal.valueOf(100))
                .withTSPName(TSP_TYPE)
                .withTSPType(TSP_TYPE);
        return transaction;
    }
}
