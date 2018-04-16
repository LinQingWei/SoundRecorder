package cn.way.soundrecorder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.os.storage.StorageManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.way.soundrecorder.ext.ExtensionHelper;
import cn.way.soundrecorder.ext.IRecordingTimeCalculationExt;
import cn.way.soundrecorder.service.SoundRecorderService;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.14
 *     desc  : Recorder class to wrapper the MediaRecorder.
 * </pre>
 */

public class Recorder implements MediaRecorder.OnErrorListener {
    private static final String TAG = "SR/Recorder";
    private static final String SAMPLE_PREFIX = "record";
    public static final String SAMPLE_SUFFIX = ".tmp";
    public static final String RECORD_FOLDER = "Recording";

    // time at which latest record or play operation started
    private long mSampleStart;
    // length of current sample
    private long mSampleLength;
    private long mPreviousTime;
    private File mSampleFile;

    private MediaRecorder mRecorder;
    private RecorderListener mListener;
    private final StorageManager mStorageManager;

    private int mCurrentState = SoundRecorderService.STATE_IDLE;
    private boolean[] mSelectEffect;

    /**
     * Recorder Callback.
     */
    public interface RecorderListener {
        /**
         * State Callback.
         *
         * @param recorder  the Recorder instance
         * @param stateCode the status code
         */
        void onStateChanged(Recorder recorder, int stateCode);

        /**
         * Error Callback.
         *
         * @param recorder  the Recorder instance
         * @param errorCode the error code
         */
        void onError(Recorder recorder, int errorCode);
    }

    public Recorder(StorageManager storageManager, RecorderListener listener) {
        mStorageManager = storageManager;
        mListener = listener;
    }

    /**
     * set Recorder to initial state.
     *
     * @return
     */
    public boolean reset() {
        boolean result = true;
        synchronized (this) {
            if (null != mRecorder) {
                try {
                    // To avoid NE while mCurrentState is not prepared.
                    if (mCurrentState == SoundRecorderService.STATE_PAUSE_RECORDING
                            || mCurrentState == SoundRecorderService.STATE_RECORDING) {
                        mRecorder.stop();
                    }
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                    result = false;
                    LogUtil.e(TAG,
                            "<stopRecording> recorder illegalstate exception in recorder.stop()");
                } finally {
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
            }
        }
        mSampleFile = null;
        mPreviousTime = 0;
        mSampleLength = 0;
        mSampleStart = 0;
        // add for some error case for example pause or goon recording failed.
        mCurrentState = SoundRecorderService.STATE_IDLE;

        return result;
    }

    /**
     * Start recording.
     *
     * @param context       the context
     * @param params        the recording parameters.
     * @param fileSizeLimit the file size limitation
     * @return the record result
     */
    public boolean startRecording(Context context, RecordParamsSetting.RecordParams params, int fileSizeLimit) {
        LogUtil.i(TAG, "<startRecording> begin");
        if (SoundRecorderService.STATE_IDLE != mCurrentState) {
            return false;
        }
        reset();

        if (!createRecordingFile(params.mExtension)) {
            LogUtil.i(TAG, "<startRecording> createRecordingFile return false");
            return false;
        }
        if (!initAndStartMediaRecorder(context, params, fileSizeLimit)) {
            LogUtil.i(TAG, "<startRecording> initAndStartMediaRecorder return false");
            return false;
        }
        mSampleStart = SystemClock.elapsedRealtime();
        setCurrentState(SoundRecorderService.STATE_RECORDING);
        LogUtil.i(TAG, "<startRecording> end");
        return true;
    }

    /**
     * Pause the recording.
     *
     * @return the pause result
     */
    public boolean pauseRecording() {
        if ((SoundRecorderService.STATE_RECORDING != mCurrentState) || (null == mRecorder)) {
            mListener.onError(this, SoundRecorderService.STATE_ERROR_CODE);
            return false;
        }
        try {
            mRecorder.pause();
        } catch (RuntimeException e) {
            LogUtil.e(TAG, "<pauseRecording> IllegalArgumentException");
            handleException(false, e);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        }
        mPreviousTime += SystemClock.elapsedRealtime() - mSampleStart;
        setCurrentState(SoundRecorderService.STATE_PAUSE_RECORDING);
        return true;
    }

    /**
     * Resume the recording.
     *
     * @return the resume result
     */
    public boolean goonRecording() {
        if ((SoundRecorderService.STATE_PAUSE_RECORDING != mCurrentState) || (null == mRecorder)) {
            return false;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.resume();
        } catch (RuntimeException exception) {
            LogUtil.e(TAG, "<goOnRecording> IllegalArgumentException");
            exception.printStackTrace();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        }

        mSampleStart = SystemClock.elapsedRealtime();
        setCurrentState(SoundRecorderService.STATE_RECORDING);
        return true;
    }

    /**
     * Stop recording.
     *
     * @return the stop result
     */
    public boolean stopRecording() {
        LogUtil.i(TAG, "<stopRecording> start");
        if (((SoundRecorderService.STATE_PAUSE_RECORDING != mCurrentState) &&
                (SoundRecorderService.STATE_RECORDING != mCurrentState)) || (null == mRecorder)) {
            LogUtil.i(TAG, "<stopRecording> end 1");
            mListener.onError(this, SoundRecorderService.STATE_ERROR_CODE);
            return false;
        }
        boolean isAdd = (SoundRecorderService.STATE_RECORDING == mCurrentState) ? true : false;
        synchronized (this) {
            try {
                if (mCurrentState != SoundRecorderService.STATE_IDLE) {
                    mRecorder.stop();
                }
            } catch (RuntimeException exception) {
                /** modified for stop recording failed. */
                handleException(false, exception);
                mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
                LogUtil.e(TAG,
                        "<stopRecording> recorder illegalstate exception in recorder.stop()");
            } finally {
                if (null != mRecorder) {
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
                if (isAdd) {
                    mPreviousTime += SystemClock.elapsedRealtime() - mSampleStart;
                }
                mSampleLength = mPreviousTime;
                LogUtil.i(TAG, "<stopRecording> mSampleLength in ms is " + mPreviousTime);
                LogUtil.i(TAG, "<stopRecording> mSampleLength in s is = " + mSampleLength);
                setCurrentState(SoundRecorderService.STATE_IDLE);
            }
        }

        LogUtil.i(TAG, "<stopRecording> end 2");
        return true;
    }

    private boolean initAndStartMediaRecorder(
            Context context, RecordParamsSetting.RecordParams recordParams, int fileSizeLimit) {
        LogUtil.i(TAG, "<initAndStartMediaRecorder> start");
        try {
            /**
             * Changed to catch the IllegalStateException and NullPointerException.
             * And the IllegalStateException will be caught and handled in RuntimeException
             */
            mSelectEffect = recordParams.mAudioEffect;
            IRecordingTimeCalculationExt ext =
                    ExtensionHelper.getRecordingTimeCalculationExt(context);
            mRecorder = ext.getMediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(recordParams.mOutputFormat);
            mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
            mRecorder.setAudioEncoder(recordParams.mAudioEncoder);
            mRecorder.setAudioChannels(recordParams.mAudioChannels);
            mRecorder.setAudioEncodingBitRate(recordParams.mAudioEncodingBitRate);
            mRecorder.setAudioSamplingRate(recordParams.mAudioSamplingRate);
            if (fileSizeLimit > 0) {
                mRecorder.setMaxFileSize(fileSizeLimit);
            }
            mRecorder.setOnErrorListener(this);
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException exception) {
            LogUtil.e(TAG, "<initAndStartMediaRecorder> IO exception");
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        } catch (NullPointerException exception) {
            /**
             * used to catch the null pointer exception in ALPS01226113,
             * and never show any toast or dialog to end user. Because this
             * error just happened when fast tapping the file list button
             * after tapping record button(which triggered by tapping the
             * recording button in audio play back view).@{
             */
            handleException(true, exception);
            return false;
        } catch (IllegalStateException exception) {
            LogUtil.e(TAG, "<initAndStartMediaRecorder> RuntimeException");
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDER_OCCUPIED);
            return false;
        } catch (RuntimeException exception) {
            LogUtil.e(TAG, "<initAndStartMediaRecorder> RuntimeException");
            handleException(true, exception);
            mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
            return false;
        }
        LogUtil.i(TAG, "<initAndStartMediaRecorder> end");
        return true;
    }

    private boolean createRecordingFile(String extension) {
        LogUtil.i(TAG, "<createRecordingFile> begin");
        String myExtension = extension + SAMPLE_SUFFIX;
        File sampleDir = null;
        if (null == mStorageManager) {
            return false;
        }
        sampleDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        LogUtil.i(TAG, "<createRecordingFile> sd card directory is:" + sampleDir);
        String sampleDirPath = sampleDir.getAbsolutePath() + File.separator + RECORD_FOLDER;
        sampleDir = new File(sampleDirPath);

        // find a available name of recording folder,
        // Recording/Recording(1)/Recording(2)
        int dirID = 1;
        while ((null != sampleDir) && sampleDir.exists() && !sampleDir.isDirectory()) {
            sampleDir = new File(sampleDirPath + '(' + dirID + ')');
            dirID++;
        }

        if ((null != sampleDir) && !sampleDir.exists() && !sampleDir.mkdirs()) {
            LogUtil.i(TAG, "<createRecordingFile> make directory [" + sampleDir.getAbsolutePath()
                    + "] fail");
        }

        boolean isCreateSuccess = true;
        try {
            if (null != sampleDir) {
                LogUtil.i(TAG, "<createRecordingFile> sample directory  is:"
                        + sampleDir.toString());
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String time = simpleDateFormat.format(new Date(System.currentTimeMillis()));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(SAMPLE_PREFIX).append(time).append(myExtension);
            String name = stringBuilder.toString();
            mSampleFile = new File(sampleDir, name);
            isCreateSuccess = mSampleFile.createNewFile();
            LogUtil.i(TAG, "<createRecordingFile> creat file success is " + isCreateSuccess);
            LogUtil.i(TAG, "<createRecordingFile> mSampleFile.getAbsolutePath() is: "
                    + mSampleFile.getAbsolutePath());
        } catch (IOException e) {
            mListener.onError(this, ErrorHandle.ERROR_CREATE_FILE_FAILED);
            LogUtil.e(TAG, "<createRecordingFile> io exception happens");
            e.printStackTrace();
            isCreateSuccess = false;
        } finally {
            LogUtil.i(TAG, "<createRecordingFile> end");
            return isCreateSuccess;
        }
    }

    /**
     * M: Handle Exception when call the function of MediaRecorder.
     *
     * @param isDeleteSample need to delete sample file or not
     * @param exception      the exception info
     */
    public void handleException(boolean isDeleteSample, Exception exception) {
        LogUtil.i(TAG, "<handleException> the exception is: " + exception);
        exception.printStackTrace();
        if (isDeleteSample && mSampleFile != null) {
            mSampleFile.delete();
        }
        try {
            if (mRecorder != null) {
                mRecorder.reset();
            }
        } catch (IllegalStateException ex) {
            LogUtil.i(TAG, "Excpetion " + ex);
        } finally {
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        LogUtil.i(TAG, "<onError> errorType = " + what + "; extraCode = " + extra);
        stopRecording();
        mListener.onError(this, ErrorHandle.ERROR_RECORDING_FAILED);
    }

    /**
     * get the current amplitude of MediaRecorder, used by VUMeter.
     *
     * @return
     */
    public int getMaxAmplitude() {
        synchronized (this) {
            if (null == mRecorder) {
                return 0;
            }
            return (SoundRecorderService.STATE_RECORDING != mCurrentState) ? 0 : mRecorder
                    .getMaxAmplitude();
        }
    }

    /**
     * get how long time we has recorded.
     *
     * @return the record length, in millseconds
     */
    public long getCurrentProgress() {
        if (SoundRecorderService.STATE_RECORDING == mCurrentState) {
            long current = SystemClock.elapsedRealtime();
            return (long) (current - mSampleStart + mPreviousTime);
        } else if (SoundRecorderService.STATE_PAUSE_RECORDING == mCurrentState) {
            return (long) (mPreviousTime);
        }
        return 0;
    }

    private void setCurrentState(int currentState) {
        if (currentState == mCurrentState) {
            return;
        }

        mCurrentState = currentState;
        if (mListener != null) {
            mListener.onStateChanged(this, currentState);
        }
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public long getSampleLength() {
        return mSampleLength;
    }

    public File getSampleFile() {
        return mSampleFile;
    }

    /**
     * get the file path of current sample file.
     *
     * @return
     */
    public String getSampleFilePath() {
        return (null == mSampleFile) ? null : mSampleFile.getAbsolutePath();
    }
}
