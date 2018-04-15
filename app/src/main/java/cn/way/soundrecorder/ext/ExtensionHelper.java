package cn.way.soundrecorder.ext;

import android.content.Context;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : Helper class to call plug-in interface.
 * </pre>
 */

public class ExtensionHelper {
    private static IRecordingTimeCalculationExt sRecordingTimeCalculationExt = null;

    /**
     * Get an implementation of IQualityLevel.
     *
     * @param context the application context
     * @return an implementation of IQualityLevel
     */
    public static IQualityLevel getExtension(Context context) {
        return new OpQualityLevel(context);
    }

    /**
     * Get an implementation of IRecordingTimeCalculationExt.
     *
     * @param context application context
     * @return an implementation of IRecordingTimeCalculationExt.
     */
    public static IRecordingTimeCalculationExt getRecordingTimeCalculationExt(Context context) {
        if (sRecordingTimeCalculationExt == null) {
            sRecordingTimeCalculationExt = new OpRecordingTimeCalculationExt(context);
        }
        return sRecordingTimeCalculationExt;
    }
}
