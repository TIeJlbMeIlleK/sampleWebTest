package ru.iitgroup.tests.webdriver;

/**
 * Справочники правил.
 */
public enum Table {

    VIP_CLIENTS_CARD_NUMBER("(Rule_tables) VIP клиенты НомерКарты"),
    VIP_CLIENTS_BIC_ACCOUNT("(Rule_tables) VIP клиенты БИКСЧЕТ"),
    PROHIBITED_RECIPIENTS_CARD_NUMBER("(Rule_tables) Запрещенные получатели НомерКарты"),
    PROHIBITED_RECIPIENTS_BIC_ACCOUNT("(Rule_tables) Запрещенные получатели БИКСЧЕТ"),
    SUSPICIOUS_DEVICES_IMSI("(Rule_tables) Подозрительные устройства IMSI"),
    ALLOWED_RECIPIENTS_PHONE_NUMBER("(Rule_tables) Разрешенные получатели Номер Телефона"),
    VIP_LIST_CLIENTS_LINKED_TO_DBO_CLIENT("(Rule_tables) Список VIP клиентов с привязкой к Клиенту ДБО"),
    SUSPICIOUS_BANKS_BIN("(Rule_tables) Подозрительные банки BIN"),
    SUSPICIOUS_DEVICES_IDENTIFIER_FOR_VENDOR("(Rule_tables) Подозрительные устройства IdentifierForVendor"),
    SUSPICIOUS_DEVICES_DEVICE_FINGER_PRINT("(Rule_tables) Подозрительные устройства DeviceFingerPrint"),
    SUSPICIOUS_BANK("(Rule_tables) Подозрительный банк"),
    SUSPICIOUS_IP("(Rule_tables) Подозрительные IP адреса"),
    SUSPICIOUS_RECIPIENTS_BIC_ACCOUNT("(Rule_tables) Подозрительные получатели БИКСЧЕТ"),
    PROHIBITED_RECIPIENTS_INN("(Rule_tables) Запрещенные получатели ИНН"),
    SUSPICIOUS_RECIPIENTS_INN("(Rule_tables) Подозрительные получатели ИНН"),
    SUSPICIOUS_RECIPIENTS_CARD_NUMBER("(Rule_tables) Подозрительные получатели НомерКарты"),
    PROHIBITED_RECIPIENTS_PHONE_NUMBER("(Rule_tables) Запрещенные получатели НомерТелефона"),
    SUSPICIOUS_DEVICES_IMEI("(Rule_tables) Подозрительные устройства IMEI");

    private final String tableName;

    Table(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return this.tableName;
    }
}

