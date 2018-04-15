package cn.way.soundrecorder.util;

import android.os.Build;
import android.util.Log;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : Log helper
 * </pre>
 */

public class LogUtil {
    private static final String TAG = "SR_LOG";
    private static final String SR_LOG_KEY = "ap.sr.debug";
    private static final String SR_LOG_DISABLE_STATE = "0";
    private static final String SR_LOG_STATE_DEF = "1";
    private static final boolean SR_LOG_DISABLED =
            Util.getPropString(SR_LOG_KEY, SR_LOG_STATE_DEF).equals(SR_LOG_DISABLE_STATE);

    private static final boolean SR_XLOG_ENABLED = true;
    private static final String SR_XLOG_TAG_PREFIX = "@Way_";
    private static final boolean USER_BUILD = "user".equals(Build.TYPE);

    private LogUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    private static boolean isSrLogDisabled() {
        return USER_BUILD && SR_LOG_DISABLED;
    }

    private static String getSrXLogTag(String tag) {
        StringBuilder xLogTag = new StringBuilder(SR_XLOG_TAG_PREFIX);
        xLogTag.append(tag);

        return xLogTag.toString();
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @return print log result
     */

    public static final int v(String tag, String msg) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.v(getSrXLogTag(tag), msg);
        } else {
            result = Log.v(tag, msg);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @param tr  throwable object
     * @return print log result
     */
    public static final int v(String tag, String msg, Throwable tr) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.v(getSrXLogTag(tag), msg, tr);
        } else {
            result = Log.v(tag, msg, tr);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @return print log result
     */
    public static final int d(String tag, String msg) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.d(getSrXLogTag(tag), msg);
        } else {
            result = Log.d(tag, msg);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @param tr  throwable object
     * @return print log result
     */
    public static final int d(String tag, String msg, Throwable tr) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.d(getSrXLogTag(tag), msg, tr);
        } else {
            result = Log.d(tag, msg, tr);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @return print log result
     */
    public static final int i(String tag, String msg) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.i(getSrXLogTag(tag), msg);
        } else {
            result = Log.i(tag, msg);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @param tr  throwable object
     * @return print log result
     */
    public static final int i(String tag, String msg, Throwable tr) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.i(getSrXLogTag(tag), msg, tr);
        } else {
            result = Log.i(tag, msg, tr);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @return print log result
     */
    public static final int w(String tag, String msg) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.w(getSrXLogTag(tag), msg);
        } else {
            result = Log.w(tag, msg);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @param tr  throwable object
     * @return print log result
     */
    public static final int w(String tag, String msg, Throwable tr) {
        if (isSrLogDisabled()) {
            return -1;
        }
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.w(getSrXLogTag(tag), msg, tr);
        } else {
            result = Log.w(tag, msg, tr);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @return print log result
     */
    public static final int e(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.e(getSrXLogTag(tag), msg);
        } else {
            result = Log.e(tag, msg);
        }
        return result;
    }

    /**
     * Log helper function.
     *
     * @param tag log tag
     * @param msg log message
     * @param tr  throwable object
     * @return print log result
     */
    public static final int e(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.e(getSrXLogTag(tag), msg, tr);
        } else {
            result = Log.e(tag, msg, tr);
        }
        return result;
    }
}
