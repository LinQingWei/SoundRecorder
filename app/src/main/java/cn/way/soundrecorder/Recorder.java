package cn.way.soundrecorder;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.14
 *     desc  : Recorder class to wrapper the MediaRecorder.
 * </pre>
 */

public class Recorder implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private static final String SAMPLE_PREFIX = "recording";
    private static final String SAMPLE_PATH_KEY = "sample_path";
    private static final String SAMPLE_LENGTH_KEY = "sample_length";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;

    private int mState = IDLE_STATE;

    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int IN_CALL_RECORD_ERROR = 3;

    private OnStateChangedListener mOnStateChangedListener;

    // time at which latest record or play operation started
    private long mSampleStart;
    // length of current sample
    private int mSampleLength;
    private File mSampleFile;

    MediaRecorder mRecorder;
    MediaPlayer mPlayer;

    public Recorder() {
    }

    public void saveState(Bundle state) {
        state.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
        state.putInt(SAMPLE_LENGTH_KEY, mSampleLength);
    }

    public void restoreState(Bundle state) {
        String samplePath = state.getString(SAMPLE_PATH_KEY);
        if (samplePath == null) {
            return;
        }
        int sampleLength = state.getInt(SAMPLE_LENGTH_KEY, -1);
        if (sampleLength == -1) {
            return;
        }

        File file = new File(samplePath);
        if (!file.exists())
            return;
        if (mSampleFile != null
                && mSampleFile.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0) {
            return;
        }

        delete();
        mSampleFile = file;
        mSampleLength = sampleLength;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is deleted.
     */
    public void delete() {
        stop();

        if (mSampleFile != null) {
            mSampleFile.delete();
        }

        mSampleFile = null;
        mSampleLength = 0;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is left on disk and will
     * be reused for a new recording.
     */
    public void clear() {
        stop();

        mSampleLength = 0;

        signalStateChanged(IDLE_STATE);
    }

    public void startRecording(Context context, int outputFileFormat, String extension) {
        stop();

        if (mSampleFile == null) {
            File sampleDir = Environment.getExternalStorageDirectory();
            if (!sampleDir.canWrite()) {
                // Workaround for broken sdcard support on the device.
                sampleDir = new File("/sdcard/sdcard");
            }

            try {
                mSampleFile = File.createTempFile(SAMPLE_PREFIX, extension, sampleDir);
            } catch (IOException e) {
                setError(SDCARD_ACCESS_ERROR);
                return;
            }
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(outputFileFormat);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }

        try {
            mRecorder.start();
        } catch (RuntimeException e) {
            AudioManager audioMngr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            final boolean isInCall = ((audioMngr.getMode() == AudioManager.MODE_IN_CALL) ||
                    (audioMngr.getMode() == AudioManager.MODE_IN_COMMUNICATION));
            if (isInCall) {
                setError(IN_CALL_RECORD_ERROR);
            } else {
                setError(INTERNAL_ERROR);
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }

        mSampleStart = System.currentTimeMillis();
        setState(RECORDING_STATE);
    }

    public void stopRecording() {
        if (mRecorder == null) {
            return;
        }

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        mSampleLength = (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        setState(IDLE_STATE);
    }

    public void startPlayback() {
        stop();

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mSampleFile.getAbsolutePath());
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IllegalArgumentException e) {
            setError(INTERNAL_ERROR);
            mPlayer = null;
            return;
        } catch (IOException e) {
            setError(SDCARD_ACCESS_ERROR);
            mPlayer = null;
            return;
        }

        mSampleStart = System.currentTimeMillis();
        setState(PLAYING_STATE);
    }

    public void stopPlayback() {
        if (mPlayer == null) {
            // we were not in playback
            return;
        }

        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        setState(IDLE_STATE);
    }

    public void stop() {
        stopRecording();
        stopPlayback();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stop();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        stop();
        setError(SDCARD_ACCESS_ERROR);
        return true;
    }

    public int getMaxAmplitude() {
        if (RECORDING_STATE != mState) {
            return 0;
        }

        return mRecorder.getMaxAmplitude();
    }

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListener = onStateChangedListener;
    }

    public int getProgress() {
        if (RECORDING_STATE == mState || PLAYING_STATE == mState) {
            return (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        }

        return 0;
    }

    private void setError(int error) {
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onError(error);
        }
    }

    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(state);
        }
    }

    private void setState(int state) {
        if (state == mState) {
            return;
        }

        mState = state;
        signalStateChanged(mState);
    }

    public int getState() {
        return mState;
    }

    public int getSampleLength() {
        return mSampleLength;
    }

    public File getSampleFile() {
        return mSampleFile;
    }

    public interface OnStateChangedListener {
        void onStateChanged(int state);

        void onError(int error);
    }
}
