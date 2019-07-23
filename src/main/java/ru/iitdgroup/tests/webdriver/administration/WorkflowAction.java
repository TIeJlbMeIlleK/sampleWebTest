package ru.iitdgroup.tests.webdriver.administration;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.iitdgroup.tests.webdriver.ic.AbstractEdit;

public class WorkflowAction extends AbstractEdit<WorkflowAction> {

    public WorkflowAction(RemoteWebDriver driver) {
        super(driver);
    }

    public WorkflowRecord save() {
        driver.findElementById("btnSave").click();
        waitUntil("//div[text()='Actions']");
        return new WorkflowRecord(driver);
    }

    public WorkflowAction setDisplayName(String displayName) {
        fillInputText("Display name:", displayName);
        return getSelf();
    }

    public WorkflowAction setUniqueName(String uniqueName) {
        fillInputText("Unique name:", uniqueName);
        return getSelf();
    }

    public WorkflowAction setDescription(String description) {
        fillInputText("Description:", description);
        return getSelf();
    }

    public WorkflowAction setType(WorkflowActionType type) {
        select("Type:", type.getVisibleName());
        return getSelf();
    }

    public WorkflowAction clearAllStates() {
        clearFromStates();
        clearToStates();
        return getSelf();
    }

    public WorkflowAction clearFromStates() {
        clearState(WorkflowTransition.FROM);
        return getSelf();
    }

    public WorkflowAction clearToStates() {
        clearState(WorkflowTransition.TO);
        return getSelf();
    }

    private void clearState(WorkflowTransition transition) {
        driver.findElementById(transition.getId())
                .findElements(By.tagName("input"))
                .stream()
                .filter(webElement ->
                        webElement.isSelected()
                                && !webElement.getAttribute("type").equals("radio"))
                .findAny()
                .ifPresent(element -> {
                    element.click();
                    sleep(1);
                    clearState(transition);
                });
    }

    public WorkflowAction addFromState(String state) {
        selectTransition(WorkflowTransition.FROM, state);
        return getSelf();
    }

    public WorkflowAction addFromState(WorkflowActionState state) {
        selectTransition(WorkflowTransition.FROM, state.getVisibleName());
        return getSelf();
    }

    public WorkflowAction addFromState(WorkflowActionState state, WorkflowActionResolution... resolutions) {
        addFromState(state);
        for (WorkflowActionResolution resolution : resolutions) {
            selectTransition(WorkflowTransition.FROM, resolution.getVisibleName());
        }
        return getSelf();
    }

    public WorkflowAction addToState(WorkflowActionState state) {
        selectTransition(WorkflowTransition.TO, state.getVisibleName());
        return getSelf();
    }

    public WorkflowAction addToState(String state) {
        selectTransition(WorkflowTransition.TO, state);
        return getSelf();
    }

    public WorkflowAction addToState(WorkflowActionState state, WorkflowActionResolution... resolutions) {
        addToState(state);
        for (WorkflowActionResolution resolution : resolutions) {
            selectTransition(WorkflowTransition.TO, resolution.getVisibleName());
        }
        return getSelf();
    }

    @Override
    public WorkflowAction getSelf() {
        return this;
    }

    private void selectTransition(WorkflowTransition state, String transition) {
        driver.findElementByXPath(
                String.format(
                        "//div[@id='%s']//following::span[text()='%s']/parent::td/parent::tr//following::input[1]",
                        state.getId(),
                        transition))
                .click();
        sleep(1);
    }

    private enum WorkflowTransition {
        FROM("fromStatesTbl"),
        TO("toStateTbl");

        private final String id;

        WorkflowTransition(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }


    public enum WorkflowActionType {
        MANUAL("Manual"),
        AUTOMATIC("Automatic");

        private final String visibleName;

        WorkflowActionType(String visibleName) {
            this.visibleName = visibleName;
        }

        public String getVisibleName() {
            return visibleName;
        }
    }

    public enum WorkflowActionState {
        KEEP_CURRENT_STATE("Keep Current State"),
        ANY_STATE("Any State"),
        NEW("Новая"),
        PROCESSED("Обработано"),
        NOT_CLASSIFIED("Не классифицировано"),
        SUSPICIOUS("Подозрительная");

        private final String visibleName;

        WorkflowActionState(String visibleName) {
            this.visibleName = visibleName;
        }

        public String getVisibleName() {
            return visibleName;
        }
    }

    public enum WorkflowActionResolution {
        ANY_RESOLUTION("Any Resolution"),
        FRAUD("Мошенничество"),
        CANCELED("Отклонено"),
        APPROVED("Подтверждено");

        private final String visibleName;

        WorkflowActionResolution(String visibleName) {
            this.visibleName = visibleName;
        }

        public String getVisibleName() {
            return visibleName;
        }
    }
}
