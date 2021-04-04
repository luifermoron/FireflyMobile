package io.github.subhamtyagi.ocr;

import android.util.Log;

import java.util.List;

import io.github.subhamtyagi.ocr.texthandler.BaseGetText;
import io.github.subhamtyagi.ocr.texthandler.GetExactText;
import io.github.subhamtyagi.ocr.texthandler.GetSimilarText;

import static io.github.subhamtyagi.ocr.texthandler.BaseGetText.DEFAULT_VALUE;


public class TagParser {
    private static final String TAG = "TagParser";

    public static BaseGetText buildGetExactText(String rawText) {
        return new GetExactText(rawText);
    }

    public static BaseGetText buildGetSimilarText(String rawText, String regexValue) {
        return new GetSimilarText(rawText).setValueRegex(regexValue);
    }

    public String findTextOn(List<String> tags, BaseGetText baseGetText) {

        for(String tag: tags) {
            String text = baseGetText.getText(tag);
            Log.d(TAG, "Text:" + text);
            Log.d(TAG, "Tag:" + tag);
            if(!text.equals(DEFAULT_VALUE))
                return text;
        }

        return DEFAULT_VALUE;
    }
}
