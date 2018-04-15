package cn.way.soundrecorder.ext;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : customize the quality level.
 * </pre>
 */

public interface IQualityLevel {
    /**
     * Get parameter for recording quality configuration.
     *
     * @return array of {Encoder, Channels, BitRate, Rate, Format}
     * @internal
     */
    int[][] getQualityLevelParams();

    /**
     * Apply specific strings to quality settings menu.
     *
     * @return array of {SettingLabel}
     * @internal
     */
    String[] getQualityLevelStrings();

    /**
     * Configure default setting for recording quality.
     *
     * @return the quality level
     * @internal
     */
    int getDefaultQualityLevel();
}
