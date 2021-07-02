package ru.iitdgroup.tests.cases.BIQ_8994;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.dbo.transaction.TransactionDataType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.Transaction;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_03_EntrustedTransfer extends RSHBCaseTest {

    private static final String RULE_NAME = "R01_WR_03_EntrustedTransfer";
    private static final String RULE_NAME_GRAY_IP = "R01_ExR_05_GrayIP";
    private static final String TABLE_SUSPICIOUS_IP = "(Rule_tables) Подозрительные IP адреса";
    private static final String TABLE_TRUSTED_RECIPIENTS = "(Rule_tables) Доверенные получатели";
    private static final String SUSPICIOUS_IP = "215.198.10.1";
    private static final String CARD = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String CARD_TRUSTED = "427863" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String ACCOUNT = "105634" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String INN = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private static final String PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String SERVICE = "МТС мобильная связь";
    private static final String PROVIDER = "МТС";
    private final String[][] names = {{"Анастасия", "Смирнова", "Витальевна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "1. Включить ExR_05_GrayIP" +
                    "2. Включить WR_03_EntrustedTransfer: Крупный перевод: 5000; Период: 10; Стат парам: 0.95" +
                    "-- добавить в него правило Исключение ExR_05_GrayIP" +
                    "3. В справочник \"Подозрительный IP\" внести IP адрес, с указанного IP отправлять транзакцию"
    )
    public void enableRules() {
        getIC().locateRules()
                .selectVisible()
                .deactivate()
                .selectRule(RULE_NAME_GRAY_IP)
                .activate();
        getIC().locateRules()
                .editRule(RULE_NAME)
                .fillCheckBox("Active:", true)
                .fillInputText("Крупный перевод:", "5000")
                .fillInputText("Период серии в минутах:", "10")
                .fillInputText("Статистический параметр Обнуления (0.95):", "0,95")
                .save()
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .attachPersonalExceptions("ExR_05_GrayIP")
                .sleep(10);

        getIC().locateTable(TABLE_SUSPICIOUS_IP)
                .deleteAll()
                .addRecord()
                .fillInputText("IP устройства:", SUSPICIOUS_IP)
                .save();
    }

    @Test(
            description = "Создаем клиента" +
                    "4. От Клиента №1 в справочник \"Доверенные получатели\" внесен доверенный получатель",
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
            }
        } catch (JAXBException | IOException e) {
            throw new IllegalStateException(e);
        }

        getIC().locateTable(TABLE_TRUSTED_RECIPIENTS)
                .deleteAll()
                .addRecord()
                .fillUser("ФИО Клиента:", clientIds.get(0))
                .fillInputText("Имя получателя:", "Сергей")
                .fillInputText("Номер карты получателя:", CARD_TRUSTED)
                .fillInputText("Номер банковского счета получателя:", ACCOUNT)
                .fillInputText("БИК банка получателя:", BIK)
                .fillInputText("ИНН получателя:", INN)
                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", PHONE)
                .fillInputText("Наименование сервиса:", SERVICE)
                .fillInputText("Наименование провайдера сервис услуги:", PROVIDER)
                .save();
    }

    @Test(
            description = "1. Отправить Транзакцию №1 Оплата услуг с подозрительного IP",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -60);
        Transaction transaction = getTransferServicePayment();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сработало персональное исключение 'ExR_05_GrayIP' белого правила");
        assertLastTransactionRuleApplyPersonalException(RULE_NAME_GRAY_IP, TRIGGERED, "IP адрес найден в Сером списке");

    }

    @Test(
            description = "2. Отправить Транзакцию №2 Оплата услуг без контейнера IpAddress" +
                    "3. ВЫключить ExR_05_GrayIP, но не убирать с правила WR_03_EntrustedTransfer",
            dependsOnMethods = "step1"
    )
    public void step2() {
        time.add(Calendar.MINUTE, 3);
        Transaction transaction = getTransferServicePayment();
        transaction.getData().getTransactionData()
                .getClientDevice()
                .getAndroid()
                .withIpAddress(null);
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сработало персональное исключение 'ExR_05_GrayIP' белого правила");
        assertLastTransactionRuleApplyPersonalException(RULE_NAME_GRAY_IP, FEW_DATA, "В конфигурации устройства нет IP адреса");

        getIC().locateRules()
                .selectRule(RULE_NAME_GRAY_IP)
                .deactivate()
                .sleep(20);
    }

    @Test(
            description = "4. Отправить Транзакцию №3 Оплата услуг на доверенного получателя: номер телефона." +
                    "5. Зайти в правило WR_03_EntrustedTransfer удалить из параметра добавленное правило " +
                    "Исключение ExR_05_GrayIP, включить ExR_05_GrayIP.",
            dependsOnMethods = "step2"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransferServicePayment();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "В списке разрешенных найдены совпадающие параметры");

        getIC().locateRules()
                .selectRule(RULE_NAME_GRAY_IP)
                .activate()
                .editRule(RULE_NAME)
                .save()
                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
                .sleep(20);
    }

    @Test(
            description = "6. Отправить Транзакцию №4 Оплата услуг на доверенного получателя",
            dependsOnMethods = "step3"
    )
    public void step4() {
        time.add(Calendar.MINUTE, 15);
        Transaction transaction = getTransferServicePayment();
        sendAndAssert(transaction);
        assertLastTransactionRuleApply(TRIGGERED, "В списке разрешенных найдены совпадающие параметры");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME;
    }

    private Transaction getTransferServicePayment() {
        Transaction transaction = getTransaction("testCases/Templates/SERVICE_PAYMENT_Android.xml");
        transaction.getData().getServerInfo().withPort(8050);
        TransactionDataType transactionDataType = transaction.getData().getTransactionData()
                .withRegular(false)
                .withVersion(1L)
                .withInitialSourceAmount(BigDecimal.valueOf(10000))
                .withDocumentSaveTimestamp(new XMLGregorianCalendarImpl(time))
                .withDocumentConfirmationTimestamp(new XMLGregorianCalendarImpl(time));
        transactionDataType
                .getClientIds()
                .withDboId(clientIds.get(0)).withLoginHash(clientIds.get(0));
        transactionDataType
                .getServicePayment()
                .withProviderName(PROVIDER)
                .withServiceName(SERVICE)
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00))
                .withSourceCardNumber(CARD)
                .getAdditionalField()
                .get(0)
                .withId("ACCOUNT")
                .withName("По номеру телефона")
                .withValue(PHONE);
        transactionDataType
                .getClientDevice()
                .getAndroid()
                .withIpAddress(SUSPICIOUS_IP);
        return transaction;
    }
}
