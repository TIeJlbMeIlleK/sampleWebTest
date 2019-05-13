package ru.iitgroup.tests.dbdriver;

public abstract class Context {
    public final Database database;

    protected Context(Database database) {
        this.database = database;
    }
}
