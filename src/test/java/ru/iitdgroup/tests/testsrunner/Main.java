package ru.iitdgroup.tests.testsrunner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Main extends Application {
    public static final String DEFAULT_TESTS_PATH = "src/test/java/ru/iitdgroup/tests/cases";
    @FXML
    private TextField testsPath;
    @FXML
    private ListView<String> testsList;
    @FXML
    private ListView<String> outputView;
    @FXML
    private Button runTestsBtn;
    @FXML
    private TextArea console;
    private final ObservableList<String> testsListItems = FXCollections.observableArrayList();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private Stage primaryStage;
    private TestsRunner testsRunner;

    private boolean DEBUG_THREADS = false;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Path path = Paths.get("resources/testsRunner/TestsRunner_ui.fxml");
        Parent root = FXMLLoader.load(path.toUri().toURL());
        Scene scene = new Scene(root, 1200, 600);
        primaryStage = stage;
        stage.setTitle("Tests Runner");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Инициализация полей, ui элементов. Гарантируется, что в initialize все FXML поля связаны с элементами из .fxml файла
     */
    @FXML
    public void initialize() {
        testsRunner = new TestsRunner(this::outputMessage);
        testsList.setItems(testsListItems);
        testsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);//выбор нескольких папок с нажатием CTRL

        PrintStream ps = new PrintStream(new PrintToTextArea(console), true);
        System.setOut(ps);
        System.setErr(ps);
        reloadTestsPackages(DEFAULT_TESTS_PATH);
    }

    /**
     * загружает список папок в testsList
     *
     * @param testsPath корневая папка, относительно которой происходит загрузка подпапок
     */
    private void reloadTestsPackages(String testsPath) {
        Path path = Paths.get(testsPath);
        testsList.getItems().clear();
        try (Stream<Path> paths = Files.walk(path, 1)) {
            paths
                    .skip(1)
                    .filter(Files::isDirectory)
                    .forEach(path1 -> {
                        testsList.getItems().add(path1.toString());
                    });
        } catch (IOException e) {
            showErrorMessage(e.toString(), this.testsPath);
        }
    }

    /**
     * Показывает окно с ошибкой
     *
     * @param text   текст ошибки
     * @param anchor элемент под которым появляется окно
     */
    private void showErrorMessage(String text, Node anchor) {
        ContextMenu errorMessage = new ContextMenu();
        errorMessage.getItems().add(new MenuItem(text));
        errorMessage.show(anchor, Side.RIGHT, 30, 30);
    }

    @FXML
    private void findBtnClick(ActionEvent event) {
        reloadTestsPackages(testsPath.getText());
        event.consume();
    }

    @FXML
    private void browseTestsFolder(ActionEvent event) {
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            testsPath.setText(selectedDirectory.getAbsolutePath());
            reloadTestsPackages(testsPath.getText());
        }
        event.consume();
    }

    @FXML
    private void runTestsBtnClick(ActionEvent event) {
        ObservableList s = testsList.getSelectionModel().getSelectedItems();
        System.out.println(s);
        if (s == null || s.size() == 0) {
            event.consume();
            return;
        }


        //outputView.getItems().clear();
//            testsRunner.testPackages(s);
        testsRunner.testPackagesAsync(s);


        event.consume();
    }

    public void outputMessage(String message) {
        if (DEBUG_THREADS) {
            try {
                outputView.getItems().add(Thread.currentThread().getName() + "\t" + message);
            } catch (IllegalStateException e) {

            }
        } else {
            Platform.runLater(() -> {
                outputView.getItems().add(message);
            });
        }
    }

    @FXML
    private void clearResult(ActionEvent event) {
        outputView.getItems().clear();
        event.consume();
    }

    @FXML
    private void testStop(ActionEvent event) {
        outputView.getItems().add("Принудительная остановка");
        testsRunner.stopAsyncTests();
        event.consume();
    }

    private class PrintToTextArea extends OutputStream {
        private TextArea textarea;

        public PrintToTextArea(TextArea console) {
            textarea = console;
        }

        @Override
        public void write(int b) throws IOException {

            Platform.runLater(() -> {
                textarea.appendText(String.valueOf((char) b));
            });
        }
    }
}
