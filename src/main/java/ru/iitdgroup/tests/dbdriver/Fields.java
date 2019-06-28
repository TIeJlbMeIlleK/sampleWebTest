package ru.iitdgroup.tests.dbdriver;

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

    public Joins innerJoin(String tableName, String leftCondition, String rightCondition) {
        Joins joins = new Joins(database);
        joins.innerJoin(tableName, leftCondition, rightCondition);
        return joins;
    }
}
