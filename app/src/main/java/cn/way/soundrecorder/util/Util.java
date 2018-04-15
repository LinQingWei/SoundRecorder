package cn.way.soundrecorder.util;

import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class Util {
    private static final String TAG = "SR_Util";

    private Util() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }


    public static Boolean getPropBoolean(String key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("getBoolean", String.class, Boolean.class);
            value = (Boolean) (get.invoke(c, key, value));
        } catch (Exception e) {
            Log.d(TAG, "get property error, " + e.getMessage());
        }
        Log.d(TAG, "get property, " + key + " = " + value);
        return value;
    }

    public static int getPropInt(String key, int defaultValue) {
        int value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("getInt", String.class, Integer.class);
            value = (Integer) (get.invoke(c, key, value));
        } catch (Exception e) {
            Log.d(TAG, "get property error, " + e.getMessage());
        }
        Log.d(TAG, "get property, " + key + " = " + value);
        return value;
    }

    public static String getPropString(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, value));
        } catch (Exception e) {
            Log.d(TAG, "get property error, " + e.getMessage());
        }
        Log.d(TAG, "get property, " + key + " = " + value);
        return value;
    }

    public static String getVolumeState(StorageManager storageManager, String mountPoint) {
        String state = null;

        if (mountPoint == null) {
            return null;
        }

        try {
            Method getVolumeState = storageManager.getClass().getMethod(
                    "getVolumeState", String.class);
            state = (String) getVolumeState.invoke(storageManager, mountPoint);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return state;
    }

    /**
     * Check if storage is mounted or not.
     *
     * @return return true if storage is mounted, otherwise return false
     */
    public static boolean isStorageMounted(StorageManager storageManager) {
        String storageState = Util.getVolumeState(storageManager,
                Environment.getExternalStorageDirectory().getAbsolutePath());
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            return true;
        }
        return false;
    }
}
