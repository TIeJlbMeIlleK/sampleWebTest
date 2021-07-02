package ru.iitdgroup.tests.cases.BIQ_8994;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.testng.annotations.Test;
import ru.iitdgroup.intellinx.crosschannel.tranantifraudcheckrequest.ServicePaymentType;
import ru.iitdgroup.intellinx.crosschannel.tranantifraudcheckrequest.TranAntiFraudCheckType;
import ru.iitdgroup.tests.apidriver.Client;
import ru.iitdgroup.tests.apidriver.TransactionEspp;
import ru.iitdgroup.tests.cases.RSHBCaseTest;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WR_103_Espp_EntrustedTransfer extends RSHBCaseTest {

    private static final String RULE_NAME_ESPP = "R01_WR_103_Espp_EntrustedTransfer";
    private static final String RULE_NAME_AttentionClient_ESPP = "R01_ExR_08_ESPP_AttentionClient";
    private static final String TABLE_Special_attention = "(Rule_tables) Список клиентов с пометкой особое внимание";
    private static final String TABLE_Trusted_recipients = "(Rule_tables) Доверенные получатели";
    private static final String TRUSTED_PHONE = "79" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 9);
    private static final String ACCOUNT_NUMBER = "42768888" + (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 12);
    private static final String BIK = (ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 10);
    private final String[][] names = {{"Вероника", "Жукова", "Игоревна"}};
    private final GregorianCalendar time = new GregorianCalendar();
    private final List<String> clientIds = new ArrayList<>();

    @Test(
            description = "1. Включить ExR_08_ESPP_AttentionClient (правила исключения)" +
                    "2. Включить WR_103_Espp_EntrustedTransfer (Белые правила)"
    )
    public void enableRules() {
//        getIC().locateRules()
//                .selectVisible()
//                .deactivate()
//                .selectRule(RULE_NAME_AttentionClient_ESPP)
//                .activate();
//        getIC().locateRules()
//                .editRule(RULE_NAME_ESPP)
//                .fillCheckBox("Active:", true)
//                .fillInputText("Крупный перевод:", "5000")
//                .fillInputText("Период серии в минутах:", "10")
//                .save()
//                .getGroupPersonalExceptionsEndDetach("Персональные Исключения")
//                .sleep(5);
    }

    @Test(
            description = "Создаем клиента" +
                    "3. У Клиента № 1 взведен флаг Особое внимание и он занесен в в справочник \"Список клиентов с пометкой особое внимание\"" +
                    "4. От Клиента №1 в справочник \"доверенные получатели\" внесен доверенный получатель",
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

//        getIC().locateTable(TABLE_Special_attention)
//                .deleteAll()
//                .addRecord()
//                .fillCheckBox("Признак «Особое внимание»:", true)
//                .fillUser("Клиент:", clientIds.get(0))
//                .save();
//        getIC().locateTable(TABLE_Trusted_recipients)
//                .deleteAll()
//                .addRecord().fillUser("ФИО Клиента:", clientIds.get(0))
//                .fillInputText("Имя получателя:", "Егор Ильич Иванов")
//                .fillInputText("Номер карты получателя:", "42760000000000002365")
//                .fillInputText("Номер банковского счета получателя:", ACCOUNT_NUMBER)
//                .fillInputText("БИК банка получателя:", BIK)
//                .fillInputText("Наименование сервиса:", "Оплата услуг")
//                .fillInputText("Наименование провайдера сервис услуги:", "МТС")
//                .fillInputText("Номер лицевого счёта/Телефон/Номер договора с сервис провайдером:", TRUSTED_PHONE)
//                .save();
    }

    @Test(
            description = "Отправить Транзакцию №2 ЕСПП на доверенного получателя от клиента №1" +
                    "3. Добавить в Белые правила : Персональное Исключение :" +
                    "-- ExR_08_ESPP_AttentionClient в WR_103_Espp_EntrustedTransfer.",
            dependsOnMethods = "addClient"
    )
    public void step1() {
        time.add(Calendar.MINUTE, -30);
        TransactionEspp transactionEspp = getTransactionEspp();
        sendEsppAndAssert(transactionEspp);
        assertLastTransactionRuleApply(TRIGGERED, "Перевод на доверенного получателя");

        getIC().locateRules()
                .editRule(RULE_NAME_ESPP)
                .save()
                .attachPersonalExceptions("ExR_08_ESPP_AttentionClient")
                .sleep(25);
    }

    @Test(
            description = "4. Отправить от клиента №1 транзакцию №2 на доверенного получателя",
            dependsOnMethods = "step1"
    )
    public void step3() {
        time.add(Calendar.MINUTE, 11);
        TransactionEspp transactionEspp = getTransactionEspp();
        sendAndAssert(transactionEspp);
        assertLastTransactionRuleApply(NOT_TRIGGERED, "Сработало персональное исключение 'ExR_08_ESPP_AttentionClient' белого правила");
        assertLastTransactionRuleApplyPersonalException(RULE_NAME_AttentionClient_ESPP, TRIGGERED, "Клиент с пометкой Особое внимание");
    }

    @Override
    protected String getRuleName() {
        return RULE_NAME_ESPP;
    }

    private TransactionEspp getTransactionEspp() {
        ArrayList<ServicePaymentType.PaymentParameters.Parameter> parameters = new ArrayList<>();
        parameters.add(new ServicePaymentType.PaymentParameters.Parameter().withName("PayeeAccount").withValue(TRUSTED_PHONE));
        parameters.add(new ServicePaymentType.PaymentParameters.Parameter().withName("R_CorpBIC").withValue(BIK));
        parameters.add(new ServicePaymentType.PaymentParameters.Parameter().withName("R_CorpBankAccount").withValue(ACCOUNT_NUMBER));

        TransactionEspp esppTrans = getTransactionESPP("testCases/Templates/ESPP_transaction.xml");
        TranAntiFraudCheckType transactionEspp = esppTrans.getData();
        transactionEspp
                .withClientId(clientIds.get(0))
                .withEksId(clientIds.get(0))
                .withBranchId(clientIds.get(0));
        transactionEspp
                .withRequestType("3")
                .withPointId(5)
                .withPointExternalId("Proc")
                .withDocumentNumber((ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE) + "").substring(0, 4))
                .withDocumentTimestamp(new XMLGregorianCalendarImpl(time))
                .getServicePayment()
                .withServiceKind(1)
                .withProviderName("МТС")
                .withServiceName("Оплата услуг")
                .withAmountInSourceCurrency(BigDecimal.valueOf(1000.00))
                .getPaymentParameters()
                .withParameter(parameters);
        return esppTrans;

    }
}
