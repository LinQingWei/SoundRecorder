package cn.way.soundrecorder;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

import cn.way.soundrecorder.service.SoundRecorderService;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : we split player from recorder, the player is only responsible for play back.
 * </pre>
 */

public class Player implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private static final String TAG = "SR/Player";
    private MediaPlayer mPlayer;
    private String mCurrentFilePath;
    private PlayerListener mListener;

    /**
     * Player Interface.
     */
    public interface PlayerListener {
        /**
         * Error Callback.
         *
         * @param player    the instance of Player
         * @param errorCode the error code
         */
        void onError(Player player, int errorCode);

        /**
         * State Change Callback.
         *
         * @param player    the instance of Player
         * @param stateCode the state code
         */
        void onStateChanged(Player player, int stateCode);
    }

    /**
     * Constructor of player, only PlayListener needed.
     *
     * @param listener
     */
    public Player(PlayerListener listener) {
        mListener = listener;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.i(TAG, "<onCompletion>");
        stopPlayback();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mListener != null) {
            mListener.onError(this, ErrorHandle.ERROR_PLAYING_FAILED);
        }
        return true;
    }

    /**
     * set the path of audio file which will play, used by
     * {@link cn.way.soundrecorder.service.SoundRecorderService}.
     *
     * @param filePath
     */
    public void setCurrentFilePath(String filePath) {
        mCurrentFilePath = filePath;
    }

    /**
     * start play the audio file which is set in setCurrentFilePath.
     *
     * @return the result of play back, success or fail
     */
    public boolean startPlayback() {
        if (null == mCurrentFilePath) {
            return false;
        }

        File file = new File(mCurrentFilePath);
        if (!file.exists()) {
            mListener.onError(this, ErrorHandle.ERROR_FILE_DELETED_WHEN_PLAY);
            return false;
        }

        synchronized (this) {
            if (null == mPlayer) {
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(mCurrentFilePath);
                    mPlayer.setOnCompletionListener(this);
                    mPlayer.prepare();
                    mPlayer.start();
                    LogUtil.i(TAG, "<startPlayback> The length of recording file is "
                            + mPlayer.getDuration());
                    setState(SoundRecorderService.STATE_PLAYING);
                } catch (IllegalStateException e) {
                    return handleException(e);
                } catch (IOException e) {
                    return handleException(e);
                }
            }
        }
        return true;
    }

    /**
     * Handle the Exception when call the function of MediaPlayer.
     *
     * @param exception
     * @return
     */
    public boolean handleException(Exception exception) {
        LogUtil.i(TAG, "<handleException>");
        exception.printStackTrace();

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        if (mListener != null) {
            mListener.onError(this, ErrorHandle.ERROR_PLAYING_FAILED);
        }

        return true;
    }

    /**
     * pause play the audio file which is set in setCurrentFilePath.
     *
     * @return
     */
    public boolean pausePlayback() {
        if (null == mPlayer) {
            return false;
        }

        try {
            mPlayer.pause();
            setState(SoundRecorderService.STATE_PAUSE_PLAYING);
        } catch (IllegalStateException e) {
            return handleException(e);
        }

        return true;
    }

    /**
     * goon play the audio file which is set in setCurrentFilePath.
     *
     * @return
     */
    public boolean goonPlayback() {
        if (null == mPlayer) {
            return false;
        }

        try {
            mPlayer.start();
            setState(SoundRecorderService.STATE_PLAYING);
        } catch (IllegalStateException e) {
            return handleException(e);
        }
        return true;
    }

    /**
     * stop play the audio file which is set in setCurrentFilePath.
     *
     * @return
     */
    public boolean stopPlayback() {
        // we were not in playback
        synchronized (this) {
            if (null == mPlayer) {
                return false;
            }
            try {
                mPlayer.stop();
                setState(SoundRecorderService.STATE_IDLE);
            } catch (IllegalStateException e) {
                return handleException(e);
            }
            mPlayer.release();
            mPlayer = null;
        }
        return true;
    }

    /**
     * reset Player to initial state.
     */
    public void reset() {
        synchronized (this) {
            if (null != mPlayer) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
        }
        mCurrentFilePath = null;
    }

    /**
     * get the current position of audio which is playing.
     *
     * @return the current position in millseconds
     */
    public int getCurrentProgress() {
        if (null != mPlayer) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * get the duration of audio file.
     *
     * @return the duration in millseconds
     */
    public int getFileDuration() {
        if (null != mPlayer) {
            return mPlayer.getDuration();
        }
        return 0;
    }

    private void setState(int state) {
        if (mListener != null) {
            mListener.onStateChanged(this, state);
        }
    }
}
