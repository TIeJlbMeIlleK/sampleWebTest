package ru.iitdgroup.tests.dbdriver;

public abstract class Context {
    public final Database database;

    protected Context(Database database) {
        this.database = database;
    }
}
