package ru.iitdgroup.tests.dbdriver;

import org.apache.commons.lang3.tuple.Pair;

public class Sorts extends Context {

    public Sorts(Database database) {
        super(database);
    }

    public Sorts sort(String fieldName, boolean asc) {
        database.getSortFields().add(Pair.of(fieldName, asc));
        return this;
    }
}
