package io.github.subhamtyagi.ocr;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.debatty.java.stringsimilarity.JaroWinkler;


public class TagParser {
    private static final String TAG = "TagParser";
    private static final String DEFAULT_VALUE = "";
    private final String NEW_LINE = System.getProperty("line.separator");
    private String rawText;

    private static final double ACCEPTANCE_VALUE = 0.94;
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

    private String getExactText(String tag) {
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

    private String findRegex(List<String> subRawTextList, String valueRegex) {

        for (int i=0; i < subRawTextList.size(); i++) {
            String singleText = subRawTextList.get(i).trim();
            if (singleText.matches(valueRegex))
                return singleText;
        }

        return DEFAULT_VALUE;
    }

    private String getAcceptanceValue(List<String> rawTextList, int startPosition, String valueRegex) {
        if (valueRegex != null)
            return findRegex(rawTextList.subList(startPosition, rawTextList.size()), valueRegex);
        else if( startPosition + 1 < rawTextList.size())
            return rawTextList.get(startPosition + 1);
        return DEFAULT_VALUE;
    }

    private String getSimilarText(List<String> rawTextList, String tag, String valueRegex) {
        if (rawTextList.isEmpty()) return DEFAULT_VALUE;

        JaroWinkler jw = new JaroWinkler();

        for (int i= 0; i < rawTextList.size(); i++) {
            String singleText = rawTextList.get(i);
            double similarity = jw.similarity(tag, singleText);

            if (similarity > ACCEPTANCE_VALUE) {
                String value = getAcceptanceValue(rawTextList, i, valueRegex);
                if (!value.equals(DEFAULT_VALUE))
                    return value;
            }
        }

        return DEFAULT_VALUE;
    }

    public String findTextOn(List<String> tags, boolean acceptSimilarTag, String valueRegex) {

        List<String> rawTextList = Arrays.asList(rawText.split("\n\n"));

        for(String tag: tags) {
            String text = acceptSimilarTag? getSimilarText(rawTextList, tag, valueRegex) : getExactText(tag);
            Log.d(TAG, "Text:" + text);
            Log.d(TAG, "Tag:" + tag);
            if(!text.equals(DEFAULT_VALUE))
                return text;
        }

        return DEFAULT_VALUE;
    }
}
