package ru.iitdgroup.tests.webdriver.jobconfiguration;

import com.google.common.base.Joiner;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

import java.util.HashMap;
import java.util.Map;

public class JobRunEdit extends AbstractEdit<JobRunEdit> {

    private static final String DESCRIPTION_TEXT_AREA = "Description";
    private static final String PARAMETERS_TEXT_AREA = "Parameters";

    private final RemoteWebDriver driver;

    private Map<String, String> parameters = new HashMap<>();
    private String description = "";
    private int waitSeconds = 10;
    private JobStatus waitStatus = JobStatus.SUCCESS;

    public JobRunEdit(RemoteWebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    /**
     * @throws InvalidElementStateException если статус Job после указанного ожидания будет отличаться от waitStatus
     */
    public JobRunEdit run() {
        // Собрали параметры и описание
        fillTextArea(PARAMETERS_TEXT_AREA, Joiner.on("\n").withKeyValueSeparator("=").join(parameters));
        fillTextArea(DESCRIPTION_TEXT_AREA, description);
        // Submit запуска Job
        driver.findElementByXPath("//button[@type='submit']").click();

        // Ждем завершения запущенного Job
        sleep(waitSeconds);

        // Обновляем таблицу
        driver.findElementByXPath("//div[@title='Refresh']").click();
        sleep(2);

        String statusText = driver.findElementByXPath("//section[@data-region='content']//table//tbody//tr//td[@class='status-cell']")
                .getText();

        if (!statusText.equals(waitStatus.getName())) {
            throw new InvalidElementStateException(String.format("invalid job status: %s", statusText));
        }

        return new JobRunEdit(driver);
    }

    /**
     * Добавить новый параметр для запуска Job
     * @param key ключ
     * @param value значение
     * @return текущие состояние запуска Job
     */
    public JobRunEdit addParameter(String key, String value) {
        parameters.put(key, value);
        return getSelf();
    }

    /**
     * Удалить параметр из запуска Job
     * @param key ключ
     * @return текущие состояние запуска Job
     */
    public JobRunEdit removeParameter(String key) {
        parameters.remove(key);
        return getSelf();
    }

    /**
     * Задать описание
     * @param description описание
     * @return текущие состояние запуска Job
     */
    public JobRunEdit description(String description) {
        this.description = description;
        return getSelf();
    }

    /**
     * @param waitSeconds кол-во секунд, для ожидания статуса выполнения Job
     * @return текущие состояние запуска Job
     */
    public JobRunEdit waitSeconds(int waitSeconds) {
        this.waitSeconds = waitSeconds;
        return getSelf();
    }

    public JobRunEdit waitStatus(JobStatus status) {
        this.waitStatus = status;
        return getSelf();
    }

    @Override
    protected JobRunEdit getSelf() {
        return this;
    }

    public enum JobStatus {

        PENDIND("Pending"),
        RUNNING("Running"),
        STOPPING("Stopping"),
        STOPPED("Stopped"),
        ERROR("Error"),
        SUCCESS("Success"),
        TERMINATED("Terminated"),
        FATAL_ERROR("Fatal Error");

        private final String name;

        JobStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
