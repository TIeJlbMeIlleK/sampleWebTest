package ru.iitdgroup.tests.dbdriver;

public class Joins extends Context {

    public Joins(Database database) {
        super(database);
    }

    public Joins innerJoin(String tableName, String leftCondition, String rightCondition) {
        database.joins.add(String.format("INNER JOIN %s ON %s = %s", tableName, leftCondition, rightCondition));
        return this;
    }

    public With from(String tableName){
        database.from = tableName;
        return new With( database);
    }
}
