package ru.iitdgroup.tests.apidriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public abstract class ApiDriverTest {

    private final static Path TEMPLATE_PATH = Paths.get("resources");

    protected String readFile(String fileName) throws IOException {
        return Files
                .lines(TEMPLATE_PATH.resolve(fileName)
                .toAbsolutePath())
                .collect(Collectors.joining("\r\n"));
    }

}
