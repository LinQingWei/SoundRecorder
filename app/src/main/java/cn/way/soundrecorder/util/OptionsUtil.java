package cn.way.soundrecorder.util;

import android.content.Context;
import android.media.AudioManager;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : Helper class to get the option.
 * </pre>
 */

public class OptionsUtil {
    private static final String TAG = "SR/OptionsUtil";
    private static final String MTK_AUDIO_HD_REC_SUPPORT = "MTK_AUDIO_HD_REC_SUPPORT";
    private static final String MTK_AUDIO_HD_REC_SUPPORT_on = "MTK_AUDIO_HD_REC_SUPPORT=true";

    /**
     * Check if AAC encode support or not
     *
     * @return whether AAC encode support or not.
     */
    public static final boolean isAACEncodeSupport() {
        return Util.getPropBoolean("ro.have_aacencode_feature", false);
    }

    /**
     * Check if HD record support.
     *
     * @param context the context
     * @return whether HD record support or not.
     */
    public static final boolean isAudioHDRecordSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            LogUtil.d(TAG, "isAudioHDRecordSupport get audio service is null.");
            return false;
        }
        String val = am.getParameters(MTK_AUDIO_HD_REC_SUPPORT);
        return val != null && val.equalsIgnoreCase(MTK_AUDIO_HD_REC_SUPPORT_on);
    }

    /**
     * this is for emulator load, if apk is running in
     * emulator, this will return true, otherwise false.
     *
     * @return if running in emulator mode
     */
    public static final boolean isRunningInEmulator() {
        int kqmenu = Util.getPropInt("ro.kernel.qemu", 0);
        return (kqmenu == 1);
    }
}
