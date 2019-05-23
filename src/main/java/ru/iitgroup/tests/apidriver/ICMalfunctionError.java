package ru.iitgroup.tests.apidriver;

import java.nio.file.Path;

public class ICMalfunctionError extends Error {
    public ICMalfunctionError(String message) {
        super(message);
    }

    public ICMalfunctionError(String message, Throwable cause) {
        super(message, cause);
    }

    public ICMalfunctionError(Throwable cause, Path picture) {
        super(cause.getMessage()+", picture at "+picture.toAbsolutePath(), cause);
    }
}
