package cn.way.soundrecorder.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.storage.StorageManager;

import cn.way.soundrecorder.RemainingTimeCalculator;
import cn.way.soundrecorder.service.SoundRecorderService;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : Default implementation of IRecordingTimeCalculationExt.
 * It will keep the default behavior of SoundRecorder application.
 * </pre>
 */

public class DefaultRecordingTimeCalculationExt extends ContextWrapper
        implements IRecordingTimeCalculationExt {

    public DefaultRecordingTimeCalculationExt(Context base) {
        super(base);
    }

    @Override
    public void setExtras(Bundle extras) {

    }

    @Override
    public MediaRecorder getMediaRecorder() {
        return new MediaRecorder();
    }

    @Override
    public RemainingTimeCalculator getRemainingTimeCalculator(StorageManager storageManager, SoundRecorderService service) {
        return new RemainingTimeCalculator(storageManager, service);
    }
}
