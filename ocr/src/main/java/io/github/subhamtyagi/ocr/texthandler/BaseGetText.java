package io.github.subhamtyagi.ocr.texthandler;

public abstract class BaseGetText {
    public static final String DEFAULT_VALUE = "";

    protected String rawText;

    public BaseGetText(String rawText) {
        this.rawText = rawText;
    }

    public abstract String getText(String tag);
}
