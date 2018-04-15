package cn.way.soundrecorder.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class PermissionUtil {

    private PermissionUtil() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Return whether <em>you</em> have granted the permissions.
     *
     * @param context
     * @param permissions The permissions.
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isGranted(Context context, final String... permissions) {
        for (String permission : permissions) {
            if (!isGranted(context, permission)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGranted(Context context, final String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(context, permission);
    }
}
