package ru.iitdgroup.tests.apidriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Transaction {
    final static Path templates = Paths.get("resources/transactions");
    final static String tagRegexp = "(.+>)(.+)(<.+)"; // <ns4:TransactionId>99696936</ns4:TransactionId>
    private static List<String> lines;
    private String body;


    public static Transaction fromFile(String fileName) throws IOException {
        Transaction t = new Transaction();
        lines = Files.readAllLines(templates.resolve(Paths.get(fileName)));
        if (lines.isEmpty()) throw new IllegalStateException("Пустой шаблон транзакции");
        return t;
    }

//    byte[] toBytes(){
//        ByteArrayOutputStream bout = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(bout);
//        for (String line : lines) {
//            ps.println(line);
//        }
//        ps.flush();
//        return bout.toByteArray();
//    }


    @Override
    public String toString() {
        return String.join(System.lineSeparator(), lines);
    }

    public Transaction withDBOId(long newDboId) {
        replaceTag("DboId", newDboId);
        return this;
    }

    public Transaction withTransactionId(long newTransactionId) {
        replaceTag("TransactionId", newTransactionId);
        return this;
    }

    public Transaction replaceTag(String tagName, Object replaceBy) {
        final String regex = "(.+>)(.+)(<.+)";
        final Pattern pattern = Pattern.compile(regex);

        for (int i = 0; i < lines.size(); i++) {
            final Matcher matcher = pattern.matcher(lines.get(i));
            if (matcher.matches() && matcher.group(0).toUpperCase().contains(tagName.toUpperCase())) {
                lines.set(i, matcher.group(1) + replaceBy.toString() + matcher.group(3));
            }
        }
        return this;
    }

    public Transaction withCIFId(long newCIFId) {
        replaceTag("CifId", newCIFId);
        return this;
    }
}
