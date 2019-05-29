package ru.iitgroup.tests.webdriver.ruleconfiguration;

//TODO: унаследовать от ICEdit
public class RuleEdit {
    private final RuleContext parent;

    public RuleEdit(RuleContext parent) {
        this.parent = parent;
    }

    //TODO: вынести в ICEdit extends ICView
    public RuleEdit fillTextArea(String heading, String value) {
        //TODO: create code
        throw new IllegalStateException("Not implemented yet");
    }

    //TODO: вынести в ICEdit extends ICView
    public RuleEdit fillCheckBox(String heading, boolean value) {
        //TODO: create code
        throw new IllegalStateException("Not implemented yet");
    }

    public RuleContext save() {
        return parent;
    }
}
