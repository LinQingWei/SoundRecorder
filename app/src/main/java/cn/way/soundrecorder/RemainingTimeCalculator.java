package cn.way.soundrecorder;

import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;

import java.io.File;

import cn.way.soundrecorder.service.SoundRecorderService;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size.
 *
 * The reason why this is not trivial is that the file grows in blocks
 * every few seconds or so, while we want a smooth countdown.
 * </pre>
 */

public class RemainingTimeCalculator {
    private static final String TAG = "SR/RemainingTimeCalculator";

    public static final int UNKNOWN_LIMIT = 0;
    public static final int FILE_SIZE_LIMIT = 1;
    public static final int DISK_SPACE_LIMIT = 2;

    private static final int ONE_SECOND = 1000;
    private static final int BIT_RATE = 8;
    private static final float RESERVE_SAPCE = SoundRecorderService.LOW_STORAGE_THRESHOLD / 2;

    private static final String RECORDING = "Recording";

    // which of the two limits we will hit (or have fit) first
    private int mCurrentLowerLimit = UNKNOWN_LIMIT;

    private String mSDCardDirectory;
    // State for tracking file size of recording.
    private File mRecordingFile;
    private long mMaxBytes;

    // Rate at which the file grows
    private int mBytesPerSecond;

    // time at which number of free blocks last changed
    private long mBlocksChangedTime;
    // number of available blocks at that time
    private long mLastBlocks;

    // time at which the size of the file has last changed
    private long mFileSizeChangedTime = -1;
    // size of the file at that time
    private long mLastFileSize;

    // the last time run timeRemaining()
    private long mLastTimeRunTimeRemaining;
    // the last remaining time
    private long mLastRemainingTime = -1;

    private final StorageManager mStorageManager;
    // if recording has been pause
    private boolean mPauseTimeRemaining = false;
    private SoundRecorderService mService;
    private String mFilePath;

    /**
     * @param storageManager
     * @param service
     */
    public RemainingTimeCalculator(StorageManager storageManager, SoundRecorderService service) {
        mStorageManager = storageManager;
        getSDCardDirectory();
        mService = service;
    }

    /**
     * If called, the calculator will return the minimum of two estimates:
     * how long until we run out of disk space and how long until the file
     * reaches the specified size.
     *
     * @param file     the file to watch
     * @param maxBytes the limit
     */

    public void setFileSizeLimit(File file, long maxBytes) {
        mRecordingFile = file;
        mMaxBytes = maxBytes;
    }

    /**
     * Resets the interpolation.
     */
    public void reset() {
        LogUtil.i(TAG, "<reset>");
        mCurrentLowerLimit = UNKNOWN_LIMIT;
        mBlocksChangedTime = -1;
        mFileSizeChangedTime = -1;
        mPauseTimeRemaining = false;
        mLastRemainingTime = -1;
        mLastBlocks = -1;
        getSDCardDirectory();
    }

    /**
     * return byte rate, using by SoundRecorder class when store state.
     *
     * @return
     */
    public int getByteRate() {
        return mBytesPerSecond;
    }

    /**
     * in order to calculate more accurate remaining time, set
     * mPauseTimeRemaining as true when MediaRecorder pause recording.
     *
     * @param pause
     */
    public void setPauseTimeRemaining(boolean pause) {
        mPauseTimeRemaining = pause;
    }

    /**
     * Returns how long (in seconds) we can continue recording. Because the
     * remaining time is calculated by estimation, add man-made control to
     * remaining time, and make it not increase when available blocks is
     * reducing.
     *
     * @param isFirstTimeGetRemainingTime if the first time to getRemainingTime
     * @param isForStartRecording         for start recording case
     * @return the remaining time that Recorder can record
     */
    public long timeRemaining(boolean isFirstTimeGetRemainingTime, boolean isForStartRecording) {
        /**
         * Modified for SD card hot plug-in/out. Should to check the savePath
         * of the current file rather than default write path.@{
         */
        if (isForStartRecording) {
            getSDCardDirectory();
            mFilePath = null;
        } else {
            mFilePath = mService.getCurrentFilePath();
        }
        if (mFilePath != null) {
            int index = mFilePath.indexOf(RECORDING, 0) - 1;
            mSDCardDirectory = mFilePath.substring(0, index);
        }
        LogUtil.i(TAG, "timeRemaining --> mFilePath is :" + mFilePath);
        // Calculate how long we can record based on free disk space
        boolean blocksNotChangeMore = false;
        StatFs fs = null;
        long blocks = 0;
        long blockSize = 0;
        try {
            fs = new StatFs(mSDCardDirectory);
            blocks = fs.getAvailableBlocks() - 1;
            blockSize = fs.getBlockSize();
        } catch (IllegalArgumentException e) {
            fs = null;
            LogUtil.d(TAG, "stat " + mSDCardDirectory + " failed...");
            return SoundRecorderService.ERROR_PATH_NOT_EXIST;
        }
        long now = SystemClock.elapsedRealtime();
        if ((-1 == mBlocksChangedTime) || (blocks != mLastBlocks)) {
            blocksNotChangeMore = (blocks <= mLastBlocks) ? true : false;
            mBlocksChangedTime = now;
            mLastBlocks = blocks;
        } else if (blocks == mLastBlocks) {
            blocksNotChangeMore = true;
        }

        /*
         * The calculation below always leaves one free block, since free space
         * in the block we're currently writing to is not added. This last block
         * might get nibbled when we close and flush the file, but we won't run
         * out of disk.
         */

        // at mBlocksChangedTime we had this much time
        float resultTemp = ((float) (mLastBlocks * blockSize - RESERVE_SAPCE)) / mBytesPerSecond;

        // if recording has been pause, we should add pause time to mBlocksChangedTime
        if (mPauseTimeRemaining) {
            mBlocksChangedTime += (now - mLastTimeRunTimeRemaining);
            mPauseTimeRemaining = false;
        }
        mLastTimeRunTimeRemaining = now;

        // so now we have this much time
        resultTemp -= ((float) (now - mBlocksChangedTime)) / ONE_SECOND;
        long resultDiskSpace = (long) resultTemp;
        mLastRemainingTime = (-1 == mLastRemainingTime) ? resultDiskSpace : mLastRemainingTime;
        if (blocksNotChangeMore && (resultDiskSpace > mLastRemainingTime)) {
            resultDiskSpace = mLastRemainingTime;
        } else {
            mLastRemainingTime = resultDiskSpace;
        }

        if ((null == mRecordingFile) && !isFirstTimeGetRemainingTime) {
            mCurrentLowerLimit = DISK_SPACE_LIMIT;
            return resultDiskSpace;
        }

        // If we have a recording file set, we calculate a second estimate
        // based on how long it will take us to reach mMaxBytes.
        if (null != mRecordingFile) {
            mRecordingFile = new File(mRecordingFile.getAbsolutePath());
            long fileSize = mRecordingFile.length();

            if ((-1 == mFileSizeChangedTime) || (fileSize != mLastFileSize)) {
                mFileSizeChangedTime = now;
                mLastFileSize = fileSize;
            }
            long resultFileSize = (mMaxBytes - fileSize) / mBytesPerSecond;
            resultFileSize -= (now - mFileSizeChangedTime) / ONE_SECOND;
            // just for safety
            resultFileSize -= 1;
            mCurrentLowerLimit = (resultDiskSpace < resultFileSize) ? DISK_SPACE_LIMIT
                    : FILE_SIZE_LIMIT;

            return Math.min(resultDiskSpace, resultFileSize);
        }
        return 0;
    }

    /**
     * Indicates which limit we will hit (or have hit) first, by returning one
     * of FILE_SIZE_LIMIT or DISK_SPACE_LIMIT or UNKNOWN_LIMIT. We need this to
     * display the correct message to the user when we hit one of the limits.
     *
     * @return current limit is FILE_SIZE_LIMIT or DISK_SPACE_LIMIT
     */
    public int currentLowerLimit() {
        return mCurrentLowerLimit;
    }

    /**
     * Sets the bit rate used in the interpolation.
     *
     * @param bitRate the bit rate to set in bits/sec.
     */
    public void setBitRate(int bitRate) {
        mBytesPerSecond = bitRate / 8;
    }

    /**
     * initialize the SD Card Directory.
     */
    private void getSDCardDirectory() {
        if (null != mStorageManager) {
            mSDCardDirectory =
                    Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    /**
     * the remaining disk space that Record can record.
     *
     * @return the remaining disk space
     */
    public long diskSpaceRemaining() {
        StatFs fs = new StatFs(mSDCardDirectory);
        long blocks = fs.getAvailableBlocks() - 1;
        long blockSize = fs.getBlockSize();
        return (long) ((blocks * blockSize) - RESERVE_SAPCE);
    }
}
