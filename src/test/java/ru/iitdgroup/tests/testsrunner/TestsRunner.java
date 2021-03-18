package ru.iitdgroup.tests.testsrunner;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TestsRunner {
    private TestNG testng;
    private Thread threadForAsyncTests;
    private Consumer<String> output;

    public TestsRunner(Consumer<String> outputMessage) {
        testng = new TestNG();
        output = outputMessage;
        MyListener listener = new MyListener(output);
        testng.addListener((ITestNGListener) listener);
    }


    public void testPackagesAsync(List<String> packages) {
        if (threadForAsyncTests != null && threadForAsyncTests.isAlive()) {
            output.accept("Тестирование уже запущено");
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                testPackages(packages);
                return null;
            }
        };

        task.setOnFailed(wse -> wse.getSource().getException().printStackTrace());

        task.setOnSucceeded(wse -> {
            output.accept("Окончание тестирования!");
            System.out.println("Окончание тестирования!");
        });

        // Before starting our task, we need to bind our UI values to the properties on the task
//        progressBar.progressProperty().bind(task.progressProperty());
//        lblProgress.textProperty().bind(task.messageProperty());

        threadForAsyncTests = new Thread(task);
        threadForAsyncTests.start();
    }

    public void stopAsyncTests() {
        threadForAsyncTests.stop();
    }

    /**
     * Запустить на тестирование пакет (папку с java тест-классами)
     *
     * @param packagePath
     * @throws IOException
     */
    public void testPackage(String packagePath) throws IOException {
        testng = new TestNG();
        testng.addListener((ITestNGListener) listener);
        listener.reset();
        List<XmlSuite> suites = generateXmlSuite(Collections.singletonList(fetchPackageName(packagePath)));

        testng.setXmlSuites(suites);
        testng.setThreadCount(1);
        testng.run();
    }

    public void testPackages(List<String> packages) throws IOException {
        testng = new TestNG();
        testng.addListener((ITestNGListener) listener);
        listener.reset();
        ArrayList<String> packageNames = new ArrayList<>();
        for (String dir : packages) {
            packageNames.add(fetchPackageName(dir));
        }
        List<XmlSuite> suites = generateXmlSuite(packageNames);

        testng.setXmlSuites(suites);
        testng.setThreadCount(1);
        testng.run();
    }

    private String fetchPackageName(String packagePath) throws IOException {
        List<Path> files = findAllTestsInFolder(packagePath);
        if (files.size() == 0) {
            throw new IllegalArgumentException("Coudn't find any .java files in " + packagePath);
        }
        String content = FileUtils.readFileToString(files.get(0).toFile());
        List<String> allMatches = new ArrayList<>();
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
                    .forEach(files::add);
        }
        return files;
    }

    @NotNull
    private List<XmlSuite> generateXmlSuite(List<String> packages) {
        List<XmlSuite> suites = new ArrayList<>();
        XmlSuite eachSuite = new XmlSuite();
        eachSuite.setName("My Suite");
        List<XmlTest> tests = new ArrayList<>();
        XmlTest eachTest = new XmlTest();
        tests.add(eachTest);
        eachTest.setName("My test");
        eachTest.setParallel(XmlSuite.ParallelMode.NONE);
        eachTest.setThreadCount(1);

        List<XmlPackage> allPackages = new ArrayList<>();

        for (String packageName : packages) {
            XmlPackage eachPackage = new XmlPackage();
            eachPackage.setName(packageName);
            allPackages.add(eachPackage);
            eachTest.setPackages(allPackages);
        }
        eachTest.setSuite(eachSuite);
        eachSuite.setTests(tests);
        suites.add(eachSuite);
        return suites;
    }

    private static class MyListener implements IClassListener, ISuiteListener, ITestListener {
        Consumer<String> output;
        ArrayList<ITestResult> classResult;

        public MyListener(Consumer<String> outputMessage) {
            this.output = outputMessage;
        }

        @Override
        public void onBeforeClass(ITestClass testClass) {
            output.accept("=== Авто-тест:  " + testClass.getRealClass().getSimpleName() + ": ");
            classResult = new ArrayList<>();
        }

        @Override
        public void onAfterClass(ITestClass testClass) {
            int success = 0;
            int failed = 0;
            ArrayList<ITestResult> failedTests = new ArrayList<>();
            for (ITestResult r : classResult) {
                if (r.isSuccess()) {
                    success++;
                } else {
                    failed++;
                    failedTests.add(r);
                }
            }
            StringBuilder s = new StringBuilder("Успешные шаги:  ");
            s.append(success).append("\n").append("Проваленые шаги:  ").append(failed);
            if (failed > 0) {
                s.append(" :  (");
                for (ITestResult r : failedTests) {
                    s.append(r.getName()).append(", ");
                }
                s.append(")");
            }
            output.accept(s.toString());
        }

        @Override
        public void onStart(ISuite suite) {
            output.accept("Начало тестирования");
            System.out.println("Начало тестирования");
        }

        @Override
        public void onFinish(ISuite suite) {
            output.accept("=======ИТОГОВЫЙ РЕЗУЛЬТАТ=======");
            Map<String, ISuiteResult> results = suite.getResults();
            for (String key : results.keySet()) {
                ISuiteResult con = results.get(key);

                int totaltestcases = con.getTestContext().getAllTestMethods().length;
                int passtestcases = con.getTestContext().getPassedTests().size();
                int failedtestcases = con.getTestContext().getFailedTests().size();
                int skippedtestcases = con.getTestContext().getSkippedTests().size();
                int percentage = (passtestcases * 100) / totaltestcases;
                output.accept("Всего ШАГОВ : " + totaltestcases);
                output.accept("Пройденных ШАГОВ : " + passtestcases);
                output.accept("Проваленных ШАГОВ : " + failedtestcases);
                output.accept("Пропущенных ШАГОВ : " + skippedtestcases);
                output.accept("Процент успешных : " + percentage + "%");
            }
        }

        @Override
        public void onTestStart(ITestResult result) {
            if (shouldStop) {
                throw new SkipException("Skipping Test: " + result.getMethod().getDescription());
            }
        }

        @Override
        public void onTestSuccess(ITestResult result) {
            classResult.add(result);
        }

        @Override
        public void onTestFailure(ITestResult result) {
            classResult.add(result);
        }

        @Override
        public void onTestSkipped(ITestResult result) {
            classResult.add(result);
        }

        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

        }

        @Override
        public void onStart(ITestContext context) {

        }

        @Override
        public void onFinish(ITestContext context) {
        }
    }


}

