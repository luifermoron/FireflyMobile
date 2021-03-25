package io.github.subhamtyagi.ocr;

import android.util.Log;

import java.util.List;

public class TagParser {
    private static final String TAG = "TagParser";
    private static final String DEFAULT_VALUE = "";
    private final String NEW_LINE = System.getProperty("line.separator");
    private String rawText;


    public TagParser(String rawText) {
        this.rawText = rawText;
    }

    private boolean containsNewLine(String text) {
        return text.contains(NEW_LINE);
    }

    private String deleteLeadingNewLine(String text) {
        int index = 0;
        while (index < text.length() && (containsNewLine(text.substring(index, index + 1)) || Character.isWhitespace(text.substring(index, index + 1).toCharArray()[0])) ) {
            index++;
        }
        if (index >= text.length() || index == 0) return text;

        return text.substring(index);
    }

    private String getText(String tag) {
        if (rawText.isEmpty()) return DEFAULT_VALUE;
        int begining = rawText.indexOf(tag);

        if (begining == -1 || (begining + tag.length()) >= rawText.length()) return DEFAULT_VALUE;
        begining += tag.length();

        String formatedText = deleteLeadingNewLine(rawText.substring(begining));
        Log.d(TAG, "formatedText:" + formatedText);

        for(int end = 0; end < formatedText.length(); end++) {
            if (containsNewLine(formatedText.substring(0, end))) {
                Log.d(TAG, "Result:" + formatedText.substring(0, end)+"+++");
                return formatedText.substring(0, end).trim();
            }
        }

        Log.d(TAG, "Did not work:" + formatedText);
        return DEFAULT_VALUE;
    }

    public String findTextOn(List<String> tags) {
        for(String tag: tags) {
            String text = getText(tag);
            Log.d(TAG, "Text:" + text);
            Log.d(TAG, "Tag:" + tag);
            if(!text.equals(DEFAULT_VALUE))
                return text;
        }
        return DEFAULT_VALUE;
    }
}
