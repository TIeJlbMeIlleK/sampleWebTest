package ru.iitdgroup.tests.testsrunner;

import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.testng.*;
import org.testng.annotations.Test;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.IOException;
import java.lang.reflect.Method;
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

    /**
     * Запустить на тестирование пакет (папку с java тест-классами)
     *
     * @param packagePath
     * @throws IOException
     */
    public void testPackage(String packagePath) throws IOException {
        testng = new TestNG();
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
        testng.addListener(new ClassListener(output));
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
        return packageLine.substring(8, packageLine.length() - 1);
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

    public void testPackages(List<String> s) throws IOException {
        testng = new TestNG();
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

        for (String dir : s) {
            String packageName = fetchPackageName(dir);
            XmlPackage eachPackage = new XmlPackage();
            eachPackage.setName(packageName);
            allPackages.add(eachPackage);
            eachTest.setPackages(allPackages);
        }
        eachTest.setSuite(eachSuite);
        eachSuite.setTests(tests);
        suites.add(eachSuite);

        testng.addListener(new TestListener(output));
        testng.addListener(new SuiteListener(output));
        testng.addListener(new ClassListener(output));
        testng.setXmlSuites(suites);
        testng.run();
    }

    static class ClassListener implements IClassListener {
        ObservableList<String> output;

        public ClassListener(ObservableList<String> output) {
            this.output = output;
        }


        @Override
        public void onBeforeClass(ITestClass testClass) {
            output.add("=== " + testClass.getName() + " ===");
        }

        @Override
        public void onAfterClass(ITestClass testClass) {
            //output.add("Class " + testClass.getName() + " конец тестирования");
        }
    }

    static class SuiteListener implements ISuiteListener {
        ObservableList<String> output;

        public SuiteListener(ObservableList<String> output) {
            this.output = output;
        }

        @Override
        public void onStart(ISuite suite) {
            output.add("Тестирование запущено");
        }

        @Override
        public void onFinish(ISuite suite) {
            output.add("===================");
            Map<String, ISuiteResult> results = suite.getResults();
            for (String key : results.keySet()) {
                ISuiteResult con = results.get(key);

                int totaltestcases = con.getTestContext().getAllTestMethods().length;
                int passtestcases = con.getTestContext().getPassedTests().size();
                int failedtestcases = con.getTestContext().getFailedTests().size();
                int skippedtestcases = con.getTestContext().getSkippedTests().size();
                int percentage = (passtestcases * 100) / totaltestcases;
                output.add("Всего авто-тестов : " + totaltestcases);
                output.add("Пройденных авто-тестов : " + passtestcases);
                output.add("Проваленных авто-тестов : " + failedtestcases);
                output.add("Пропущенных авто-тестов : " + skippedtestcases);
                output.add("Процент успешных : " + percentage + "%");
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
            output.add(getTestName(tr).append("\nпровален").toString());
        }

        @Override
        public void onTestSkipped(ITestResult tr) {
            output.add(getTestName(tr).append("\nпропущен").toString());
        }

        @Override
        public void onTestSuccess(ITestResult tr) {
            output.add(getTestName(tr).append("\nуспешно пройден").toString());
        }

        private StringBuilder getTestName(ITestResult tr) {
            StringBuilder result = new StringBuilder();

            try {
                ITestNGMethod res = tr.getMethod();
                result.append(res.toString()).append("\n");
                Method method = res.getRealClass().getMethod(res.getMethodName());
                String description = method.getAnnotation(Test.class).description();
                result.append("(").append(description).append(")");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            return result;
        }
    }

}

