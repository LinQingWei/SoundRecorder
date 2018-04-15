package cn.way.soundrecorder.ext;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.storage.StorageManager;

import cn.way.soundrecorder.RemainingTimeCalculator;
import cn.way.soundrecorder.service.SoundRecorderService;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : The Interface to support the customization of recording time calculation.
 * </pre>
 */

public interface IRecordingTimeCalculationExt {
    /**
     * Called when host app, succeed to init from intent.
     * Host app need to pass the extras of the intent to this method.
     *
     * @param extras the extra of the intent to start SoundRecorder activity.
     */
    public void setExtras(Bundle extras);

    /**
     * Called when host app need to create a MediaRecorder object.
     * The default implementation will just new a MediaRecorder.
     * The plugin implementation may set extra attribute for this MediaRecorder.
     *
     * @return an instance of MediaRecorder object.
     */
    public MediaRecorder getMediaRecorder();

    /**
     * Called when host app need to create a RemainingTimeCalculator object.
     * The default implementation will just new a RemainingTimeCalculator.
     * The plugin implementation may return a subclass of RemainingTimeCalculator
     * to change the behavior of RemainingTimeCalculator
     *
     * @param storageManager the reference of StorageManager.
     * @param service        the reference of SoundRecorderService.
     * @return an instance of RemainingTimeCalculator or its subclass object.
     */
    public RemainingTimeCalculator getRemainingTimeCalculator(
            StorageManager storageManager,
            SoundRecorderService service);
}
