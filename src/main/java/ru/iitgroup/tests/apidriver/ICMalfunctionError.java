package ru.iitgroup.tests.apidriver;

public class ICMalfunctionError extends Error {
    public ICMalfunctionError(String message) {
        super(message);
    }

    public ICMalfunctionError(String message, Throwable cause) {
        super(message, cause);
    }
}
