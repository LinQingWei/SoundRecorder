package cn.way.soundrecorder.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class SoundRecorderUtils {
    private static final String TAG = "SR/SoundRecorderUtils";
    private static Toast sToast;

    private SoundRecorderUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * send Broadcast to stop music.
     *
     * @param context the context that call this function
     */
    public static void sendBroadcastToStopMusic(Context context) {
        if (null == context) {
            LogUtil.e(TAG, "<sendBroadcastToStopMusic> context is null");
            return;
        }
        final String commandString = "command";
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra(commandString, "pause");
        context.sendBroadcast(i);
    }

    /**
     * delete file from Media database.
     *
     * @param context  the context that call this function
     * @param filePath the file path to to deleted
     * @return delete result
     */
    public static boolean deleteFileFromMediaDB(Context context, String filePath) {
        LogUtil.i(TAG, "<deleteFileFromMediaDB> begin");
        if (null == context) {
            LogUtil.e(TAG, "<deleteFileFromMediaDB> context is null");
            return false;
        }
        if (null == filePath) {
            LogUtil.i(TAG, "<deleteFileFromMediaDB> filePath is null");
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] ids = new String[]{MediaStore.Audio.Media._ID};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MediaStore.Audio.Media.DATA);
        stringBuilder.append(" LIKE '%");
        stringBuilder.append(filePath.replaceFirst("file:///", ""));
        stringBuilder.append("'");
        final String where = stringBuilder.toString();
        Cursor cursor = query(context, base, ids, where, null, null);
        boolean res = false;
        try {
            if ((null != cursor) && (cursor.getCount() > 0)) {
                int deleteNum = resolver.delete(base, where, null);
                LogUtil.i(TAG, "<deleteFileFromMediaDB> delete " + deleteNum + " items in db");
                res = (deleteNum != 0);
            } else {
                if (cursor == null) {
                    LogUtil.e(TAG, "<deleteFileFromMediaDB>, cursor is null");
                } else {
                    LogUtil.e(TAG, "<deleteFileFromMediaDB>, cursor is:" + cursor
                            + "; cursor.getCount() is:" + cursor.getCount());
                }
            }
        } catch (IllegalStateException e) {
            LogUtil.e(TAG, "<deleteFileFromMediaDB> " + e.getMessage());
            res = false;
        } catch (SQLiteFullException e) {
            LogUtil.e(TAG, "<deleteFileFromMediaDB> " + e.getMessage());
            res = false;
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        LogUtil.i(TAG, "<deleteFileFromMediaDB> end");
        return res;
    }

    /**
     * A simple utility to do a query into the databases.
     *
     * @param context       the context that call this function
     * @param uri           data URI
     * @param projection    column collection
     * @param selection     the rule of select
     * @param selectionArgs the args of select
     * @param sortOrder     sort order
     * @return the cursor returned by resolver.query
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        if (null == context) {
            LogUtil.e(TAG, "<query> context is null");
            return null;
        }
        try {
            ContentResolver resolver = context.getContentResolver();
            if (null == resolver) {
                LogUtil.e(TAG, "<query> resolver is null");
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            LogUtil.e(TAG, ex.getMessage());
            return null;
        }
    }

    /**
     * show a toast.
     *
     * @param context the context
     * @param resId   the content that to be display
     */
    public static void getToast(Context context, int resId) {
        if (null == sToast) {
            sToast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        }
        sToast.setText(resId);
        sToast.show();
    }
}
