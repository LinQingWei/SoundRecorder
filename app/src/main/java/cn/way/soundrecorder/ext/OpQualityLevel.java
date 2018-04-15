package cn.way.soundrecorder.ext;

import android.content.Context;

import cn.way.soundrecorder.R;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : We define three quality levels and three sets of parameters for them.
 * </pre>
 */

public class OpQualityLevel extends DefaultQualityLevel {
    private static final int QUALITY_LEVEL_NUM = 3;
    private static final int CONFIG_ITEM_NUM = 5;

    public OpQualityLevel(Context context) {
        super(context);
    }

    @Override
    public int[][] getQualityLevelParams() {
        int[][] params = new int[QUALITY_LEVEL_NUM][CONFIG_ITEM_NUM];
        params[0] = this.getResources().getIntArray(R.array.operator_high_params);
        params[1] = this.getResources().getIntArray(R.array.operator_standard_params);
        params[2] = this.getResources().getIntArray(R.array.operator_low_params);
        return params.clone();
    }

    @Override
    public String[] getQualityLevelStrings() {
        String[] strs = new String[QUALITY_LEVEL_NUM];
        strs[0] = this.getResources().getString(R.string.recording_format_high);
        strs[1] = this.getResources().getString(R.string.recording_format_mid);
        strs[2] = this.getResources().getString(R.string.recording_format_low);
        return strs;
    }

    @Override
    public int getDefaultQualityLevel() {
        // default recording quality is low(the last quality level,index:2).
        return (QUALITY_LEVEL_NUM - 1);
    }
}
