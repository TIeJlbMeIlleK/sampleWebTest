package ru.iitdgroup.tests.webdriver.administration;

import org.testng.annotations.Test;
import ru.iitdgroup.rshbtest.RSHBTests;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction.WorkflowActionResolution;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction.WorkflowActionState;
import ru.iitdgroup.tests.webdriver.administration.WorkflowAction.WorkflowActionType;

public class WorkflowsTest extends RSHBTests {
    
    private static final String RECORD_NAME = "Транзакция Workflow";

    @Test
    public void testOpenRecord() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME);
    }

    @Test
    public void testOpenAction() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .openAction("Подтвердить мошенничество");
    }

    @Test
    public void testSelectAction() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .selectAction("Подтвердить мошенничество");
    }

    @Test
    public void testDeleteAction() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .deleteAction("test");
    }

    @Test
    public void testAddAction() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .addAction();
    }

    @Test
    public void testSelectStates() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .openAction("Подтвердить мошенничество")
                .clearAllStates()
                .addFromState(WorkflowActionState.ANY_STATE)
                .addToState(WorkflowActionState.PROCESSED, WorkflowActionResolution.APPROVED);
    }
    
    @Test
    public void testSetDisplayName() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .addAction()
                .setDisplayName("dname1");
    }

    @Test
    public void testSetUniqueName() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .addAction()
                .setUniqueName("uq1");
    }

    @Test
    public void testSetDescription() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .addAction()
                .setDescription("desc1");
    }

    @Test
    public void testSetType() {
        ic.locateWorkflows()
                .openRecord(RECORD_NAME)
                .addAction()
                .setType(WorkflowActionType.MANUAL);
    }
}