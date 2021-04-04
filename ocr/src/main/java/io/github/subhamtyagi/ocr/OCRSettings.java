package io.github.subhamtyagi.ocr;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.github.subhamtyagi.ocr.ocr.ImageTextReader;
import io.github.subhamtyagi.ocr.utils.Constants;
import io.github.subhamtyagi.ocr.utils.CrashUtils;
import io.github.subhamtyagi.ocr.utils.SpUtil;
import io.github.subhamtyagi.ocr.utils.Utils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.googlecode.tesseract.android.TessBaseAPI;


public class OCRSettings {

    private String mTrainingDataType;
    private String mLanguage;
    private File dirBest;
    private File dirStandard;
    private File dirFast;
    private File currentDirectory;
    private ImageTextReader mImageTextReader;
    private DownloadTrainingTask downloadTrainingTask;
    private ConvertImageToTextTask convertImageToTextTask;

    private AlertDialog dialog;
    ProgressDialog mProgressDialog;

    private CrashUtils crashUtils;
    private AppCompatActivity activity;

    private static final String TAG = "OCRSettings";
    private static boolean isRefresh = false;
    private TagParser tagParser;

    private OCRResult ocrResult = null;

    private List<String> descriptions = null;
    private List<String> amounts = null;

    private MutableLiveData<Boolean> isDownloading = new MutableLiveData<>();

    public OCRSettings(AppCompatActivity activity) {
        this.activity = activity;
        this.isDownloading.postValue(true);

        SpUtil.getInstance().init(activity);
        crashUtils = new CrashUtils(activity.getApplicationContext(), "");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            initDirectories();
            initializeOCR();
        }
    }

    public void processData(OCRResult ocrResult, List<String> descriptions, List<String> amounts) {
        Log.d(TAG, "processData:");
        Log.d(TAG, descriptions.toString());
        Log.d(TAG, amounts.toString());

        this.descriptions = descriptions;
        this.amounts = amounts;
        this.ocrResult = ocrResult;

        Intent intent = activity.getIntent();
        String uriString = intent.getStringExtra("uri_image");

        if (uriString != null) {
            Uri imageUri = Uri.parse(uriString);
            convertImageToText(imageUri);
        }
    }

    public MutableLiveData<Boolean> getIsDownloading() {
        return isDownloading;
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    private void initDirectories() {
        dirBest = new File(activity.getExternalFilesDir("best").getAbsolutePath());
        dirFast = new File(activity.getExternalFilesDir("fast").getAbsolutePath());
        dirStandard = new File(activity.getExternalFilesDir("standard").getAbsolutePath());
        dirBest.mkdirs();
        dirStandard.mkdirs();
        dirFast.mkdirs();
        currentDirectory = new File(dirBest, "tessdata");
        currentDirectory.mkdirs();
        currentDirectory = new File(dirStandard, "tessdata");
        currentDirectory.mkdirs();
        currentDirectory = new File(dirFast, "tessdata");
        currentDirectory.mkdirs();
    }

    public void initializeOCR() {
        File cf;
        mTrainingDataType = Utils.getTrainingDataType();
        mLanguage = Utils.getTrainingDataLanguage();

        switch (mTrainingDataType) {
            case "best":
                currentDirectory = new File(dirBest, "tessdata");
                cf = dirBest;
                break;
            case "standard":
                cf = dirStandard;
                currentDirectory = new File(dirStandard, "tessdata");
                break;
            default:
                cf = dirFast;
                currentDirectory = new File(dirFast, "tessdata");

        }

        if (isLanguageDataExists(mTrainingDataType, mLanguage)) {
            //region Initialize image text reader
            new Thread() {
                @Override
                public void run() {
                    try {

                        if (mImageTextReader != null) {
                            mImageTextReader.tearDownEverything();
                        }
                        mImageTextReader = ImageTextReader.geInstance(cf.getAbsolutePath(), mLanguage, null); //activityMainActivity.this::onProgressValues
                        //check if current language data is valid
                        //if it is invalid(i.e. corrupted, half downloaded, tempered) then delete it
                        if (!mImageTextReader.success) {
                            File destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, mLanguage));
                            destf.delete();
                            mImageTextReader = null;
                        } else {
                            isDownloading.postValue(false);
                            Log.d(TAG, "initializeOCR: Reader is initialize with lang:" + mLanguage);
                        }

                    } catch (Exception e) {
                        crashUtils.logException(e);
                        File destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, mLanguage));
                        destf.delete();
                        mImageTextReader = null;
                    }
                }
            }.start();
            //endregion
        } else {
            downloadLanguageData(mTrainingDataType, mLanguage);
        }
    }

    public void saveBitmapToStorage(Bitmap bitmap) {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = activity.openFileOutput("last_file.jpeg", Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fileOutputStream);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap loadBitmapFromStorage() {
        Bitmap bitmap = null;
        FileInputStream fileInputStream;
        try {
            fileInputStream = activity.openFileInput("last_file.jpeg");
            bitmap = BitmapFactory.decodeStream(fileInputStream);
            fileInputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private void convertImageToText(Uri imageUri) {
        //Utils.putLastUsedImageLocation(imageUri.toString());
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        convertImageToTextTask = new ConvertImageToTextTask();
        convertImageToTextTask.execute(bitmap);
    }

    private void downloadLanguageData(final String dataType, final String lang) {

        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(activity.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        ArrayList<String> langToDownload = new ArrayList<>();
        if (lang.contains("+")) {
            String[] lang_codes = lang.split("\\+");
            for (String lang_code : lang_codes) {
                if (!isLanguageDataExists(dataType, lang)) {
                    langToDownload.add(lang_code);
                }
            }
        }

        if (ni == null) {
            //You are not connected to Internet
            Toast.makeText(activity, activity.getString(R.string.you_are_not_connected_to_internet), Toast.LENGTH_SHORT).show();
        } else if (ni.isConnected()) {
            //region show confirmation dialog, On 'yes' download the training data.
            String msg = String.format(activity.getString(R.string.download_description), lang);
            dialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.training_data_missing)
                    .setCancelable(false)
                    .setMessage(msg)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            downloadTrainingTask = new DownloadTrainingTask();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                                downloadTrainingTask.execute(dataType, lang);
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            if (!isLanguageDataExists(dataType, lang)) {
                                //  show dialog to change language
                            }

                        }
                    }).create();
            dialog.show();
            //endregion
        } else {
            Toast.makeText(activity, activity.getString(R.string.you_are_not_connected_to_internet), Toast.LENGTH_SHORT).show();
            //You are not connected to Internet
        }
    }

    /**
     * Check if language data exists
     *
     * @param dataType data type i.e best, fast, standard
     * @param lang     language
     * @return true if language data exists
     */
    private boolean isLanguageDataExists(final String dataType, final String lang) {
        switch (dataType) {
            case "best":
                currentDirectory = new File(dirBest, "tessdata");
                break;
            case "standard":
                currentDirectory = new File(dirStandard, "tessdata");
                break;
            default:
                currentDirectory = new File(dirFast, "tessdata");

        }
        if (lang.contains("+")) {
            String[] lang_codes = lang.split("\\+");
            for (String code : lang_codes) {
                File file = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, code));
                if (!file.exists()) return false;
            }
            return true;
        } else {
            File language = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
            return language.exists();
        }
    }

    public void showOCRResult(String description, String amount) {
        if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED) && ocrResult != null) {
            ocrResult.showOCRResult(description, amount.matches("\\d*\\.?\\d+") ? amount : "");
        }
    }

    /**
     * A Async Task to convert the image into text the return the text in String
     */
    private class ConvertImageToTextTask extends AsyncTask<Bitmap, Void, String[]> {

        @Override
        protected String[] doInBackground(Bitmap... bitmaps) {
            Bitmap bitmap = bitmaps[0];
            if (!isRefresh && Utils.isPreProcessImage()) {
                bitmap = Utils.preProcessBitmap(bitmap);
                // bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 1.5), (int) (bitmap.getHeight() * 1.5), true);
            }
            isRefresh = false;
            saveBitmapToStorage(bitmap);
            String rawText = mImageTextReader.getTextFromBitmap(bitmap);
            Log.v(TAG, "RawText:" + rawText);
            String cleanText = Html.fromHtml(rawText).toString().trim();
            Log.d(TAG, cleanText);
            tagParser = new TagParser();
            String original = cleanText;
            String description = tagParser.findTextOn(descriptions, TagParser.buildGetExactText(cleanText));
            String amount =  tagParser.findTextOn(amounts, TagParser.buildGetSimilarText(cleanText,"\\d*\\.?\\d+"));

            return new String[] { original, description, amount };
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String[] values) {

            showOCRResult(values[1], values[2]);
            Toast.makeText(activity, "With Confidence:" + mImageTextReader.getAccuracy() + "%", Toast.LENGTH_SHORT).show();

            Utils.putLastUsedText(values[0]);
            Bitmap bitmap = loadBitmapFromStorage();
            if (bitmap != null) {
                // do nothing
            }
        }

    }

    /**
     * Download the training Data and save this to external storage
     */
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @SuppressLint("StaticFieldLeak")
    private class DownloadTrainingTask extends AsyncTask<String, Integer, Boolean> {
        String size;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setTitle(activity.getString(R.string.downloading));
            mProgressDialog.setMessage(activity.getString(R.string.downloading_language));
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int percentage = values[0];
            if (mProgressDialog != null) {
                mProgressDialog.setMessage(percentage + activity.getString(R.string.percentage_downloaded) + size);
                mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
                mProgressDialog = null;
            }
            initializeOCR();
        }


        @Override
        protected Boolean doInBackground(String... languages) {
            String dataType = languages[0];
            String lang = languages[1];
            boolean ret = true;
            if (lang.contains("+")) {
                String[] lang_codes = lang.split("\\+");
                for (String code : lang_codes) {
                    if (!isLanguageDataExists(dataType, code)) {
                        ret &= downloadTraningData(dataType, code);
                    }
                }
                return ret;
            } else {
                return downloadTraningData(dataType, lang);
            }
        }


        /**
         * done the actual work of download
         *
         * @param dataType data type i.e best, fast, standard
         * @param lang     language
         * @return true if success else false
         */
        private boolean downloadTraningData(String dataType, String lang) {
            boolean result = true;
            String downloadURL;
            String location;

            switch (dataType) {
                case "best":
                    downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_BEST, lang);
                    break;
                case "standard":
                    downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_STANDARD, lang);
                    break;
                default:
                    downloadURL = String.format(Constants.TESSERACT_DATA_DOWNLOAD_URL_FAST, lang);
            }

            URL url, base, next;
            HttpURLConnection conn;
            try {
                while (true) {
                    Log.v(TAG, "downloading " + downloadURL);
                    try {
                        url = new URL(downloadURL);
                    } catch (MalformedURLException ex) {
                        Log.e(TAG, "url " + downloadURL + " is bad: " + ex);
                        return false;
                    }
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    switch (conn.getResponseCode()) {
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            location = conn.getHeaderField("Location");
                            base = new URL(downloadURL);
                            next = new URL(base, location);  // Deal with relative URLs
                            downloadURL = next.toExternalForm();
                            continue;
                    }
                    break;
                }
                conn.connect();

                int totalContentSize = conn.getContentLength();
                size = Utils.getSize(totalContentSize);

                InputStream input = new BufferedInputStream(url.openStream());

                File destf = new File(currentDirectory, String.format(Constants.LANGUAGE_CODE, lang));
                destf.createNewFile();
                OutputStream output = new FileOutputStream(destf);

                byte[] data = new byte[1024 * 6];
                int count, downloaded = 0;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    downloaded += count;
                    int percentage = (downloaded * 100) / totalContentSize;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                        publishProgress(percentage);
                    }
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                result = false;
                Log.e(TAG, "failed to download " + downloadURL + " : " + e);
                e.printStackTrace();
                crashUtils.logException(e);
            }
            return result;
        }
    }
}
