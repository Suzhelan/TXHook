package moe.ore.android.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public final class DeviceIdUtils {
    private static final String TAG = DeviceIdUtils.class.getSimpleName();

    private static final String TEMP_DIR = "system_config";
    private static final String TEMP_FILE_NAME = "system_file";
    private static final String TEMP_FILE_NAME_MIME_TYPE = "application/octet-stream";
    private static final String SP_NAME = "device_info";
    private static final String SP_KEY_DEVICE_ID = "device_id";

    public static String getDeviceId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String deviceId = sharedPreferences.getString(SP_KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(deviceId)) {
            return deviceId;
        }
        deviceId = getIMEI(context);
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = createUUID(context);
        }
        sharedPreferences.edit()
                .putString(SP_KEY_DEVICE_ID, deviceId)
                .apply();
        return deviceId;
    }

    private static String createUUID(Context context) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Uri externalContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();
            String[] projection = new String[]{
                    MediaStore.Downloads._ID
            };
            String selection = MediaStore.Downloads.TITLE + "=?";
            String[] args = new String[]{
                    TEMP_FILE_NAME
            };
            Cursor query = contentResolver.query(externalContentUri, projection, selection, args, null);
            if (query != null && query.moveToFirst()) {
                Uri uri = ContentUris.withAppendedId(externalContentUri, query.getLong(0));
                query.close();

                InputStream inputStream = null;
                BufferedReader bufferedReader = null;
                try {
                    inputStream = contentResolver.openInputStream(uri);
                    if (inputStream != null) {
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        uuid = bufferedReader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.TITLE, TEMP_FILE_NAME);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, TEMP_FILE_NAME_MIME_TYPE);
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, TEMP_FILE_NAME);
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + TEMP_DIR);

                Uri insert = contentResolver.insert(externalContentUri, contentValues);
                if (insert != null) {
                    OutputStream outputStream = null;
                    try {
                        outputStream = contentResolver.openOutputStream(insert);
                        if (outputStream == null) {
                            return uuid;
                        }
                        outputStream.write(uuid.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
            File externalDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File applicationFileDir = new File(externalDownloadsDir, TEMP_DIR);
            if (!applicationFileDir.exists()) {
                if (!applicationFileDir.mkdirs()) {
                    Log.e(TAG, "文件夹创建失败: " + applicationFileDir.getPath());
                }
            }
            File file = new File(applicationFileDir, TEMP_FILE_NAME);
            if (!file.exists()) {
                FileWriter fileWriter = null;
                try {
                    if (file.createNewFile()) {
                        fileWriter = new FileWriter(file, false);
                        fileWriter.write(uuid);
                    } else {
                        Log.e(TAG, "文件创建失败：" + file.getPath());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "文件创建失败：" + file.getPath());
                    e.printStackTrace();
                } finally {
                    if (fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                FileReader fileReader = null;
                BufferedReader bufferedReader = null;
                try {
                    fileReader = new FileReader(file);
                    bufferedReader = new BufferedReader(fileReader);
                    uuid = bufferedReader.readLine();

                    bufferedReader.close();
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return uuid;
    }

    private static String getIMEI(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return null;
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return null;
            }
            @SuppressLint({"MissingPermission", "HardwareIds"}) String imei = telephonyManager.getDeviceId();
            return imei;
        } catch (Exception e) {
            return null;
        }
    }
}