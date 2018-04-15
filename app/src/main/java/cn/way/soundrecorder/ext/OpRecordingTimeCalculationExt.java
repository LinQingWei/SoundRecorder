package cn.way.soundrecorder.ext;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.storage.StorageManager;

import cn.way.soundrecorder.Recorder;
import cn.way.soundrecorder.RemainingTimeCalculator;
import cn.way.soundrecorder.service.SoundRecorderService;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : It will keep the default behavior of SoundRecorder application.
 * </pre>
 */

public class OpRecordingTimeCalculationExt extends DefaultRecordingTimeCalculationExt {
    private static final String EXTRA_MAX_DURATION = "com.android.soundrecorder.maxduration";
    private static final long MAX_DURATION_NULL = -1L;
    private long mMaxDuration = MAX_DURATION_NULL;

    public OpRecordingTimeCalculationExt(Context context) {
        super(context);
    }

    @Override
    public void setExtras(Bundle extras) {
        if (extras != null) {
            mMaxDuration = extras.getLong(EXTRA_MAX_DURATION, MAX_DURATION_NULL);
        } else {
            mMaxDuration = MAX_DURATION_NULL;
        }
    }

    @Override
    public MediaRecorder getMediaRecorder() {
        MediaRecorder recorder = super.getMediaRecorder();
        if (mMaxDuration != MAX_DURATION_NULL) {
            recorder.setMaxDuration((int) mMaxDuration);
        }
        return recorder;
    }

    @Override
    public RemainingTimeCalculator getRemainingTimeCalculator(StorageManager storageManager, SoundRecorderService service) {
        return new OpRemainingTimeCalculator(storageManager, service);
    }

    /**
     * Consider the max duration limitation.
     */
    public class OpRemainingTimeCalculator extends RemainingTimeCalculator {
        private SoundRecorderService mService;

        public OpRemainingTimeCalculator(StorageManager storageManager,
                                         SoundRecorderService service) {
            super(storageManager, service);
            mService = service;
        }

        @Override
        public long timeRemaining(boolean isFirstTimeGetRemainingTime, boolean isForStartRecording) {
            long remainingTime =
                    super.timeRemaining(isFirstTimeGetRemainingTime, isForStartRecording);
            Recorder recorder = null;
            if (mService != null) {
                recorder = mService.getRecorder();
            }

            if (mMaxDuration != MAX_DURATION_NULL && recorder != null) {
                long currentPogress = recorder.getCurrentProgress();
                long diff = mMaxDuration - currentPogress;
                if (diff > 0) {
                    diff = diff / 1000 + 1;
                }
                if (diff < 0) {
                    remainingTime = 0;
                } else {
                    remainingTime = Math.min(remainingTime, diff);
                }
            }
            return remainingTime;
        }
    }
}
