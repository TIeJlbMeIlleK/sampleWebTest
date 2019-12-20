package ru.iitdgroup.tests.dbdriver;

public class Limit extends Context {

    public Limit(Database database) {
        super(database);
    }

    public Limit limit(int offset, int limit) {
        database.setOffset(offset);
        database.setLimit(limit);
        return this;
    }

    public Limit limit(int limit) {
        return limit(0, limit);
    }
}
