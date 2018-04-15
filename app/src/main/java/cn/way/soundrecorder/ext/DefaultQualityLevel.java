package cn.way.soundrecorder.ext;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class DefaultQualityLevel extends ContextWrapper implements IQualityLevel {
    public DefaultQualityLevel(Context base) {
        super(base);
    }

    @Override
    public int[][] getQualityLevelParams() {
        return null;
    }

    @Override
    public String[] getQualityLevelStrings() {
        return null;
    }

    @Override
    public int getDefaultQualityLevel() {
        return -1;
    }
}
