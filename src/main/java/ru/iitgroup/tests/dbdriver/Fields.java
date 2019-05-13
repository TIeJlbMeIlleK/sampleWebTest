package ru.iitgroup.tests.dbdriver;

public class Fields extends Context {

    protected Fields(Database database) {
        super(database);
    }

    public Fields field(String name){
        database.selectFields.add( name);
        return this;
    }

    public With from(String tableName){
        database.from = tableName;
        return new With( database);
    }
}
