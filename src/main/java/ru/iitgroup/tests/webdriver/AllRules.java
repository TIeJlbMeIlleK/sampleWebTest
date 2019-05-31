package ru.iitgroup.tests.webdriver;

/**
 * Шаблоны правил.
 */
public enum AllRules {
    BR_01_PayeeInBlackList,
    BR_02_AbnormalSpeed,
    ExR_01_AuthenticationContactChanged,
    ExR_02_AnomalGeoPosChange,
    ExR_03_UseNewDevice,
    ExR_04_InfectedDevice,
    ExR_05_GrayIP,
    ExR_06_GrayDevice,
    ExR_07_Devices,
    ExR_08_AttentionClient,
    ExR_09_UseNewMobileDevice,
    ExR_10_AuthenticationFromSuspiciousDevice,
    GR_01_AnomalTransfer,
    GR_02_ResetBalance,
    GR_03_SeriesOneToMany,
    GR_04_OnePayerToManyPhones,
    GR_05_ManyPayerToOnePhone,
    GR_07_BigTransfer,
    GR_15_NonTypicalGeoPosition,
    GR_18_GrayBank,
    GR_19_GrayBeneficiar,
    GR_20_NewPayee,
    GR_23_GrayBIN,
    GR_24_SeriesBetweenOwnAccounts,
    IR_01_CheckDBO,
    IR_02_UncheckedClients,
    WR_01_OwnAccount,
    WR_02_BudgetTransfer,
    WR_03_EntrustedTransfer,
    WR_04_ToVip,
    WR_05_TransferOnOldAcc
}
