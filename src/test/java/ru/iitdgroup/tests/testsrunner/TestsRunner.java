package ru.iitdgroup.tests.testsrunner;

import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.testng.*;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TestsRunner {
    private TestNG testng;
    private ObservableList<String> output;

    public TestsRunner(ObservableList<String> items) {
        testng = new TestNG();
        output = items;
    }


    public void testPackage(String packagePath) throws IOException {
        testng = new TestNG();
        List<Path> files = findAllTestsInFolder(packagePath);
        String packageName = fetchPackageName(packagePath);

        List<XmlSuite> suites = new ArrayList<XmlSuite>();
        XmlSuite eachSuite = new XmlSuite();
        eachSuite.setName("My Suite");
        List<XmlTest> tests = new ArrayList<XmlTest>();
        XmlTest eachTest = new XmlTest();
        tests.add(eachTest);
        eachTest.setName("My test");
        eachTest.setParallel(XmlSuite.ParallelMode.NONE);
        eachTest.setThreadCount(1);

        List<XmlPackage> allPackages = new ArrayList<XmlPackage>();
        XmlPackage eachPackage = new XmlPackage();
        eachPackage.setName(packageName);
        allPackages.add(eachPackage);
        eachTest.setPackages(allPackages);

        eachTest.setSuite(eachSuite);
        eachSuite.setTests(tests);
        suites.add(eachSuite);

        testng.addListener(new TestListener(output));
        testng.addListener(new SuiteListener(output));
        testng.setXmlSuites(suites);
        testng.run();
    }

    private String fetchPackageName(String packagePath) throws IOException {
        List<Path> files = findAllTestsInFolder(packagePath);
        if (files.size() == 0) {
            throw new IllegalArgumentException("Coudn't find any .java files in " + packagePath);
        }
        String content = FileUtils.readFileToString(files.get(0).toFile());
        List<String> allMatches = new ArrayList<String>();
        Matcher m = Pattern.compile("package [\\w.]*;")
                .matcher(content);
        if (!m.find()) {
            return null;
        }
        String packageLine = m.group();
        return packageLine.substring(8, packageLine.length()-1);
    }

    private List<Path> findAllTestsInFolder(String folder) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(folder), 1)) {
            paths
                    //.skip(1)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .forEach(file -> {
                        files.add(file);
                    });
        }
        return files;
    }

    static class SuiteListener implements ISuiteListener {
        ObservableList<String> output;

        public SuiteListener(ObservableList<String> output) {
            this.output = output;
        }

        @Override
        public void onStart(ISuite suite) {
            output.add("Tests started");
        }


        @Override
        public void onFinish(ISuite suite)
        {
            Map<String,ISuiteResult> results=suite.getResults();
            for(String key: results.keySet())
            {
                ISuiteResult con = results.get(key);

                int totaltestcases=con.getTestContext().getAllTestMethods().length;

                int passtestcases= con.getTestContext().getPassedTests().size();
                int failedtestcases=con.getTestContext().getFailedTests().size();
                int skippedtestcases=con.getTestContext().getSkippedTests().size();
                int percentage=(passtestcases*100)/totaltestcases;
                output.add("Total test cases : "+totaltestcases);
                output.add("Passed test cases : "+passtestcases);
                output.add("Failed test cases : "+failedtestcases);
                output.add("Skipped test cases : "+skippedtestcases);
                output.add("PASS PERCENTAGE : "+percentage+"%");

            }
        }
    }

    public static class TestListener extends TestListenerAdapter {
        private int m_count = 0;
        ObservableList<String> output;

        public TestListener(ObservableList<String> output) {
            this.output = output;
        }

        @Override
        public void onTestFailure(ITestResult tr) {
            output.add(tr.getName() + " failed");
        }

        @Override
        public void onTestSkipped(ITestResult tr) {

        }

        @Override
            public void onTestSuccess(ITestResult tr) {

        }

    }

}

