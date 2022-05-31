package org.andresoviedo.util.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import org.andresoviedo.android_3d_model_engine.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
/**************************************************************************************************/
public class ContentUtils {
    /**********************************************************************************************/
    private static Map<String, Uri> documentsProvided = new HashMap<>();
    /**********************************************************************************************/
    private static ThreadLocal<Context> currentActivity = new ThreadLocal<>();
    /**********************************************************************************************/
    private static File currentDir = null;

    /**********************************************************************************************/
    public static void setThreadActivity(Context currentActivity) {
        Log.i("ContentUtils", "Current activity thread: " + Thread.currentThread().getName());

        ContentUtils.currentActivity.set(currentActivity);
    }

    /**********************************************************************************************/
    private static Context getCurrentActivity() {
        return ContentUtils.currentActivity.get();
    }

    /**********************************************************************************************/
    public static void setCurrentDir(File file) {
        ContentUtils.currentDir = file;
    }

    /**********************************************************************************************/
    public static void clearDocumentsProvided() {
        documentsProvided.clear();
    }

    /**********************************************************************************************/
    public static void provideAssets(Activity activity) {
        documentsProvided.clear();

        try {
            for (String document : activity.getAssets().list("models")) {
                addUri("/models/" + document, Uri.parse("android://" + activity.getPackageName() + "/assets/models/" + document));
                addUri(document, Uri.parse("android://" + activity.getPackageName() + "/assets/models/" + document));
            }

        } catch (IOException ex) {
            Log.e("ContentUtils", "Error listing assets from models folder", ex);
        }
    }

    /**********************************************************************************************/
    public static void addUri(String name, Uri uri) {
        documentsProvided.put(name, uri);

        Log.i("ContentUtils", "Added (" + name + ") " + uri);
    }

    /**********************************************************************************************/
    public static Uri getUri(String name) {
        return documentsProvided.get(name);
    }

    /**********************************************************************************************/
    public static InputStream getInputStream(String path) throws IOException {
        Uri uri = getUri(path);

        if (uri == null && currentDir != null) {
            uri = Uri.parse("file://" + new File(currentDir, path).getAbsolutePath());
        }

        if (uri != null) {
            return getInputStream(uri);
        }

        Log.w("ContentUtils", "Media not found: " + path);
        Log.w("ContentUtils", "Available media: " + documentsProvided);

        throw new FileNotFoundException("File not found: " + path);
    }

    /**********************************************************************************************/
    public static InputStream getInputStream(URI uri) throws IOException {
        return getInputStream(Uri.parse(uri.toURL().toString()));
    }

    /**********************************************************************************************/
    public static InputStream getInputStream(Uri uri) throws IOException {
        Log.i("ContentUtils", "Opening stream ..." + uri);

        if (uri.getScheme().equals("android")) {
            if (uri.getPath().startsWith("/assets/")) {
                final String path = uri.getPath().substring("/assets/".length());
                Log.i("ContentUtils", "Opening asset: " + path);
                return getCurrentActivity().getAssets().open(path);
            } else if (uri.getPath().startsWith("/res/drawable/")) {
                final String path = uri.getPath().substring("/res/drawable/".length()).replace(".png", "");
                Log.i("ContentUtils", "Opening drawable: " + path);
                final int resourceId = getCurrentActivity().getResources()
                        .getIdentifier(path, "drawable", getCurrentActivity().getPackageName());
                return getCurrentActivity().getResources().openRawResource(resourceId);
            } else {
                throw new IllegalArgumentException("unknown android path: " + uri.getPath());
            }
        }

        if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
            return new URL(uri.toString()).openStream();
        }

        if (uri.getScheme().equals("content")) {
            return getCurrentActivity().getContentResolver().openInputStream(uri);
        }
        return getCurrentActivity().getContentResolver().openInputStream(uri);
    }

    /**********************************************************************************************/
    public static InputStream getInputStream(int resourceId) throws IOException {
        if (getCurrentActivity() == null) throw new IllegalStateException("No current activity");

        return getCurrentActivity().getResources().openRawResource(resourceId);
    }

    /**********************************************************************************************/
    public static Intent createGetContentIntent(String mimeType) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat) {
            return createGetMultipleContentIntent(mimeType);
        }

        return createGetSingleContentIntent(mimeType);
    }

    /**********************************************************************************************/
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Intent createGetMultipleContentIntent(String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType(mimeType);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        return intent;
    }

    /**********************************************************************************************/
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Intent createGetSingleContentIntent(String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType(mimeType);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        return intent;
    }

    /**********************************************************************************************/
    public static void showDialog(Activity activity, String title, CharSequence message, String positiveButtonLabel, String negativeButtonLabel, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveButtonLabel, listener);
        builder.setNegativeButton(negativeButtonLabel, listener);
        builder.create().show();
    }

    /**********************************************************************************************/
    public static void showListDialog(Activity activity, String title, String[] options, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(title).setItems(options, listener);

        builder.create().show();
    }

    /**********************************************************************************************/
    @FunctionalInterface
    public interface Callback {
        void onClick(String asset);
    }

    /**********************************************************************************************/
    public static AlertDialog.Builder createChooserDialog(Context context, String title, CharSequence message, List<String> fileListAssets, String fileRegex, AssetUtils.Callback callback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setNegativeButton("Cancel", (DialogInterface dialog, int which) -> {
            callback.onClick(null);
        });

        final List<String> fileListModels = new ArrayList<>();

        for (String file : fileListAssets) {
            if (file.matches(fileRegex)) {
                fileListModels.add(file);
            }
        }

        final String[] fileListArray = new String[fileListModels.size()];

        for (int i = 0; i < fileListModels.size(); i++) {
            String model = fileListModels.get(i);
            fileListArray[i] = model.substring(model.lastIndexOf("/") + 1);
            if (fileListArray[i].endsWith(".index")) {
                fileListArray[i] = fileListArray[i].replace(".index", "...");
            }
        }

        builder.setItems(fileListArray, (DialogInterface dialog, int which) -> {
            documentsProvided.clear();
            for (String asset : fileListAssets) {
                documentsProvided.put(asset.substring(asset.lastIndexOf("/") + 1), Uri.parse(asset));
            }
            callback.onClick(fileListModels.get(which));
        });

        return builder;
    }

    /**********************************************************************************************/
    public static AlertDialog.Builder createChooserDialog(Context context, String title, CharSequence message, List<String> fileListAssets, Map<String, byte[]> icons, String fileRegex, AssetUtils.Callback callback) {
        if (icons == null) {
            return createChooserDialog(context, title, message, fileListAssets, fileRegex, callback);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton("Cancel", (DialogInterface dialog, int which) -> {
            callback.onClick(null);
        });

        final List<String> fileListModels = new ArrayList<>();

        for (String file : fileListAssets) {
            if (file.matches(fileRegex)) {
                fileListModels.add(file);
            }
        }

        final ArrayList<Map<String, Object>> modelList = new ArrayList<>();

        for (int i = 0; i < fileListModels.size(); i++) {
            final String url = fileListModels.get(i);
            final String filename = url.substring(url.lastIndexOf('/') + 1);
            final String label = filename.endsWith(".index") ? filename.substring(0, filename.length() - 6) + "..." : filename;
            final String icon = filename + ".jpg";
            final String icon2 = filename + ".png";

            final Map<String, Object> hashMap = new HashMap<>();

            hashMap.put("name", label);

            if (icons != null && icons.containsKey(icon)) {
                hashMap.put("image", String.valueOf(i));
                hashMap.put("bitmap", BitmapFactory.decodeStream(new ByteArrayInputStream(icons.get(icon))));
            } else if (icons != null && icons.containsKey(icon2)) {
                hashMap.put("image", String.valueOf(i));
                hashMap.put("bitmap", BitmapFactory.decodeStream(new ByteArrayInputStream(icons.get(icon2))));
            }

            hashMap.put("url", url);

            modelList.add(hashMap);
        }

        final String[] from = {"name", "image"};
        final int[] to = {R.id.textView, R.id.imageView};

        final ListAdapter textImageAdapter = new SimpleAdapter(context, modelList, R.layout.list_text_with_image, from, to) {
            public void setViewImage(ImageView v, String value) {
                v.setImageBitmap(null);
                if (value != null && value.length() > 0) {
                    try {
                        v.setImageBitmap((Bitmap) modelList.get(Integer.parseInt(value)).get("bitmap"));
                    } catch (Exception ex) {
                        Log.e("ContentUtils", ex.getMessage(), ex);
                    }
                }
            }
        };

        builder.setAdapter(textImageAdapter, (DialogInterface dialog, int which) -> {
            documentsProvided.clear();
            for (String asset : fileListAssets) {
                documentsProvided.put(asset.substring(asset.lastIndexOf("/") + 1), Uri.parse(asset));
            }
            callback.onClick((String) modelList.get(which).get("url"));
        });

        return builder;
    }

    /**********************************************************************************************/
    public static List<String> readLines(String url) {
        try {
            return readLines(new URL(url));
        } catch (MalformedURLException ex) {
            Log.e("ContentUtils", "Error", ex);
            return null;
        }
    }

    /**********************************************************************************************/
    public static List<String> readLines(URL url) {
        try {
            List<String> ret = new ArrayList<>();
            try (InputStream is = url.openStream()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                    ret.add(line);
                }
            }
            return ret;
        } catch (IOException ex) {
            Log.e("ContentUtils", "Error reading from " + url, ex);
            return null;
        }
    }

    /**********************************************************************************************/
    public static Map<String, byte[]> readFiles(URL url) {
        try {
            byte[] buffer = new byte[512];
            try (InputStream is = url.openStream()) {
                final Map<String, byte[]> ret = new HashMap<>();

                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    final String name = ze.getName();
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int readed = 0;
                    while ((readed = zis.read(buffer)) > 0) {
                        bos.write(buffer, 0, readed);
                    }
                    ret.put(name, bos.toByteArray());
                }
                return ret;
            }
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            Log.e("ContentUtils", "Error reading from " + url, ex);
            return null;
        }
    }
}