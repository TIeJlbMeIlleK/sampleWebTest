package ru.iitgroup.tests.apidriver;

public class NoSuccessStatusError extends Error {
    public NoSuccessStatusError(String message) {
        super(message);
    }

    public NoSuccessStatusError(String message, Throwable cause) {
        super(message, cause);
    }
}
