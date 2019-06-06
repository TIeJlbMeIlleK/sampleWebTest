package ru.iitgroup.tests.apidriver;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class TransactionTest {

    @Test
    public void testToString1() throws IOException {
        final String filled = Transaction.fromFile("tran1.xml")
                .withDBOId(11111111)
                .withTransactionId(222222222)
                .toString();

        final String expected = Files.lines( Transaction.templates.resolve("tran1.test").toAbsolutePath())
                .collect(Collectors.joining("\r\n"));

        assertEquals(filled,expected);

    }
}