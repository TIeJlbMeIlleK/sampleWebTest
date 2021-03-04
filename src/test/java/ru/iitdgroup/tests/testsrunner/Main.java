package ru.iitdgroup.tests.testsrunner;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.swing.text.View;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Main extends Application {
    public static final String DEFAULT_TESTS_PATH = "src/test/java/ru/iitdgroup/tests/cases";
    @FXML private TextField testsPath;
    @FXML private ListView<String> testsList;
    @FXML private ListView<String> outputView;
    private final ObservableList<String> testsListItems = FXCollections.observableArrayList();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private Stage primaryStage;
    private TestsRunner testsRunner;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Path path = Paths.get("resources/testsRunner/TestsRunner_ui.fxml");
        Parent root = FXMLLoader.load(path.toUri().toURL());
        Scene scene = new Scene(root, 800, 600);
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
        testsRunner = new TestsRunner(outputView.getItems());
        testsList.setItems(testsListItems);
        testsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);//выбор нескольких папок с нажатием CTRL
        reloadTestsPackages(DEFAULT_TESTS_PATH);
    }

    /**
     * загружает список папок в testsList
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
     * @param text текст ошибки
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
        if (s == null) {
            event.consume();
            return;
        }


        outputView.getItems().clear();
//            testsRunner.testPackages(s);
        testsRunner.startTestProcess(s);


        event.consume();
    }



    @FXML
    private void clearResult(ActionEvent event) {
        outputView.getItems().clear();
        event.consume();
    }
}
