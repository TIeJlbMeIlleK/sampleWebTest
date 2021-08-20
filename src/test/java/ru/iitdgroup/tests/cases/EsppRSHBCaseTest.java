package ru.iitdgroup.tests.cases;

public abstract class EsppRSHBCaseTest extends RSHBCaseTest{

    private static final String ESPP_NAME_TABLE_INCIDENTS   = "ESPP_INCIDENT_WRAP";
    private static final String ESPP_NAME_TABLE_TRANSACTION = "ESPP_PAYMENT_TRANSACTION";

    @Override
    protected String getNameTableIncidents() {
        return ESPP_NAME_TABLE_INCIDENTS;
    }

    @Override
    protected String getNameTableTransactions() {
        return ESPP_NAME_TABLE_TRANSACTION;
    }
}
