package cn.way.soundrecorder.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.way.soundrecorder.ErrorHandle;
import cn.way.soundrecorder.OnScreenHint;
import cn.way.soundrecorder.R;
import cn.way.soundrecorder.RecordParamsSetting;
import cn.way.soundrecorder.Recorder;
import cn.way.soundrecorder.ext.ExtensionHelper;
import cn.way.soundrecorder.ext.IRecordingTimeCalculationExt;
import cn.way.soundrecorder.fragment.SelectDialogFragment;
import cn.way.soundrecorder.service.SoundRecorderService;
import cn.way.soundrecorder.util.LogUtil;
import cn.way.soundrecorder.util.OptionsUtil;
import cn.way.soundrecorder.util.PermissionUtil;
import cn.way.soundrecorder.util.SoundRecorderUtils;
import cn.way.soundrecorder.widget.VUMeter;

public class SoundRecorderActivity extends AppCompatActivity
        implements View.OnClickListener,
        SoundRecorderService.OnEventListener,
        SoundRecorderService.OnErrorListener, SoundRecorderService.OnStateChangedListener,
        SoundRecorderService.OnUpdateTimeViewListener {
    private static final String TAG = "SR/SoundRecorder";
    private static final String NULL_STRING = "";
    private static final int OPTIONMENU_SELECT_FORMAT = 0;
    private static final int OPTIONMENU_SELECT_MODE = 1;
    private static final int OPTIONMENU_SELECT_EFFECT = 2;
    private static final int DIALOG_SELECT_MODE = 0;
    private static final int DIALOG_SELECT_FORMAT = 1;
    public static final int DIALOG_SELECT_EFFECT = 2;
    private static final int TWO_BUTTON_WEIGHT_SUM = 2;
    private static final int THREE_BUTTON_WEIGHT_SUM = 3;
    private static final int REQUEST_FILE_LIST = 1;
    private static final int TIME_BASE = 60;
    private static final long MAX_FILE_SIZE_NULL = -1L;
    private static final int TIME_NINE_MIN = 540;
    private static final int MMS_FILE_LIMIT = 190;
    private static final long ONE_SECOND = 1000;
    private static final int DONE = 100;

    private static final int PERMISSION_RECORD_AUDIO = 1;
    private static final int PERMISSION_READ_STORAGE = 2;
    private static final int PERMISSION_READ_STORAGE_LIST = 3;
    private static final int PERMISSION_WRITE_STORAGE_LIST = 4;

    private static final String AUDIO_NOT_LIMIT_TYPE = "audio/*";
    private static final String DIALOG_TAG_SELECT_MODE = "SelectMode";
    private static final String DIALOG_TAG_SELECT_FORMAT = "SelectFormat";
    private static final String DIALOG_TAG_SELECT_EFFECT = "SelectEffect";
    private static final String SOUND_RECORDER_DATA = "sound_recorder_data";
    private static final String PATH = "path";
    public static final String PLAY = "play";
    public static final String RECORD = "record";
    public static final String INIT = "init";
    public static final String DOWHAT = "dowhat";
    public static final String EMPTY = "";
    public static final String ERROR_CODE = "errorCode";

    private int mSelectedFormat = -1;
    private int mSelectedMode = -1;
    private boolean[] mSelectEffectArray = new boolean[3];
    private boolean[] mSelectEffectArrayTemp = new boolean[3];

    private String mRequestedType = AUDIO_NOT_LIMIT_TYPE;
    private String mTimerFormat;
    private String mFileName = "";
    private String mDoWhat;
    private String mDoWhatFilePath;
    private long mMaxFileSize = -1L;
    private boolean mRunFromLauncher = true;
    private boolean mHasFileSizeLimitation;
    private boolean mBackPressed;
    private boolean mOnSaveInstanceStateHasRun;
    private PowerManager.WakeLock mWakeLock;
    private boolean mIsStopService;
    // used for saving record file when SoundRecorder launch from other
    // application
    private boolean mSetResultAfterSave = true;
    private SharedPreferences mPrefs;
    private boolean mIsButtonDisabled;

    private Menu mMenu;
    private final SoundRecorderHandler mHandler = new SoundRecorderHandler(this);

    private Button mAcceptButton;
    private Button mDiscardButton;
    private ImageButton mRecordButton;
    private ImageButton mPlayButton;
    private ImageButton mStopButton;
    private ImageButton mFileListButton;
    private ImageButton mPauseRecordingButton;
    // image view at the left of mStateTextView
    private ImageView mRecordingStateImageView;
    // image view at the left of mRecordingFileNameTextView
    private ImageView mPlayingStateImageView;
    // message below the state message
    private TextView mRemainingTimeTextView;
    // state message with LED
    private TextView mStateTextView;
    private TextView mTimerTextView;
    private TextView mRecordingFileNameTextView;
    private ProgressBar mStateProgressBar;
    private LinearLayout mExitButtons;
    private VUMeter mVUMeter;
    private LinearLayout mButtonParent;
    private OnScreenHint mStorageHint;
    private ImageView mFirstLine;
    private ImageView mSecondLine;
    private FrameLayout mFrameLayout;
    private boolean mIsRecordStarting;
    private boolean mDoRecordWhenResume;

    private boolean mFileFromList;
    private boolean mResumeNeedRefresh;
    private boolean mSavingRecordFileFromMms;

    private SoundRecorderService mService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            LogUtil.i(TAG, "<onServiceConnected> Service connected");
            mService = ((SoundRecorderService.SoundRecorderBinder) arg1).getService();
            initWhenHaveService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            LogUtil.i(TAG, "<onServiceDisconnected> Service dis connected");
            mService = null;
        }
    };

    private DialogInterface.OnClickListener mSelectFormatListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichItemSelect) {
                    LogUtil.i(TAG, "<mSelectFormatListener onClick>");
                    setSelectedFormat(whichItemSelect);
                    dialog.dismiss();
                }
            };

    private DialogInterface.OnClickListener mSelectModeListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichItemSelect) {
                    LogUtil.i(TAG, "<mSelectModeListener onClick>");
                    setSelectedMode(whichItemSelect);
                    dialog.dismiss();
                }
            };

    private DialogInterface.OnClickListener mSelectEffectOkListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            mSelectEffectArray = mSelectEffectArrayTemp.clone();
            if (null != mService) {
                mService.setSelectEffectArray(mSelectEffectArray);
            }
        }
    };

    private DialogInterface.OnMultiChoiceClickListener
            mSelectEffectMultiChoiceClickListener = new DialogInterface.OnMultiChoiceClickListener() {

        @Override
        public void onClick(DialogInterface arg0, int arg1, boolean arg2) {
            mSelectEffectArrayTemp[arg1] = arg2;
            if (null != mService) {
                mService.setSelectEffectArrayTmp(mSelectEffectArrayTemp);
            }
        }
    };

    private SoundRecorderService.OnUpdateButtonState mButtonUpdater =
            new SoundRecorderService.OnUpdateButtonState() {
                @Override
                public void updateButtonState(boolean enable) {
                    if (!enable) {
                        SoundRecorderActivity.this.disableButton();
                    }
                }
            };

    @Override
    public void onEvent(int eventCode) {
        switch (eventCode) {
            case SoundRecorderService.EVENT_SAVE_SUCCESS:
                LogUtil.i(TAG, "<onEvent> EVENT_SAVE_SUCCESS");
                Uri uri = mService.getSaveFileUri();
                if (null != uri) {
                    mHandler.sendEmptyMessage(SoundRecorderService.STATE_SAVE_SUCESS);
                }
                if (!mRunFromLauncher) {
                    LogUtil.i(TAG, "<onEvent> mSetResultAfterSave = " + mSetResultAfterSave);
                    if (mSetResultAfterSave) {
                        setResult(RESULT_OK, new Intent().setData(uri)
                                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                        LogUtil.i(TAG, "<onEvent> finish");
                        LogUtil.i(TAG, "<onEvent> Activity = " + this.toString());
                        finish();
                    } else {
                        mSetResultAfterSave = true;
                    }
                }
                mService.reset();
                mHandler.sendEmptyMessage(mService.getCurrentState());
                long mEndSaveTime = System.currentTimeMillis();
                Log.i(TAG, "[Performance test][SoundRecorder] recording save end [" + mEndSaveTime
                        + "]");
                break;
            case SoundRecorderService.EVENT_DISCARD_SUCCESS:
                LogUtil.i(TAG, "<onEvent> EVENT_DISCARD_SUCCESS");
                if (mRunFromLauncher) {
                    mService.reset();
                    mHandler.sendEmptyMessage(mService.getCurrentState());
                } else {
                    mService.reset();
                    LogUtil.i(TAG, "<onEvent> finish");
                    LogUtil.i(TAG, "<onEvent> Activity = " + this.toString());
                    finish();
                }
                break;
            case SoundRecorderService.EVENT_STORAGE_MOUNTED:
                LogUtil.i(TAG, "<onEvent> EVENT_STORAGE_MOUNTED");
                // remove error dialog after sd card mounted
                removeOldFragmentByTag(ErrorHandle.ERROR_DIALOG_TAG);
                break;
            default:
                LogUtil.i(TAG, "<onEvent> event out of range, event code = " + eventCode);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i(TAG, "<onCreate> start, Activity = " + this.toString());
        if (!handleIntent(getIntent())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        setContentView(R.layout.activity_sound_recorder);

        if (!mRunFromLauncher) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            LogUtil.e(TAG, "<onCreate> PowerManager == " + pm);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            }
        }

        // Initial the record parameters shared preferences when firstly use.
        RecordParamsSetting.initRecordParamsSharedPreference(this);
        mResumeNeedRefresh = true;
        LogUtil.i(TAG, "<onCreate> end");
    }


    private boolean handleIntent(Intent intent) {
        LogUtil.i(TAG, "<handleIntent> start: " + intent);
        if (intent != null) {
            /**
             * check if SoundRecorder is start by launcher or start by
             * SoundRecorderService
             */
            String action = intent.getAction();
            if (action == null) {
                LogUtil.i(TAG, "<handleIntent> the action is null");
                mRunFromLauncher = true;
            } else {
                mRunFromLauncher = (action.equals(Intent.ACTION_MAIN))
                        || (action.equals("cn.way.soundrecorder.SoundRecorder"));
            }
            String typeString = intent.getType();
            if (null != typeString) {
                if (RecordParamsSetting.isAvailableRequestType(typeString)) {
                    mRequestedType = typeString;
                } else {
                    LogUtil.i(TAG, "<handleIntent> return false");
                    return false;
                }
            }
            mMaxFileSize = intent.getLongExtra(MediaStore.Audio.Media.EXTRA_MAX_BYTES,
                    MAX_FILE_SIZE_NULL);
            /** if mMaxFileSize != -1, set mHasFileSizeLimitation as true. */
            mHasFileSizeLimitation = (mMaxFileSize != MAX_FILE_SIZE_NULL);

            /* Pass extras to recording time calculation Extension */
            IRecordingTimeCalculationExt ext =
                    ExtensionHelper.getRecordingTimeCalculationExt(getApplicationContext());
            ext.setExtras(intent.getExtras());
        }
        LogUtil.i(TAG, "<handleIntent> end");
        return true;
    }

    private void initWhenHaveService() {
        LogUtil.i(TAG, "<initWhenHaveService> start");
        mService.setErrorListener(this);
        mService.setEventListener(this);
        mService.setStateChangedListener(this);
        mService.setShowNotification(mRunFromLauncher);
        // Add for update time view through implements the listener defined by SoundRecorderService.
        mService.setUpdateTimeViewListener(this);
        initResourceRefs();
        // if run from other application, we will stop recording and auto
        // save the recording file
        // and reset SoundRecorder to initial state
        if (!mRunFromLauncher) {
            mService.doStop(mButtonUpdater);
            if (mService.isCurrentFileWaitToSave()) {
                // set mSetResultAfterSave = false ,and set
                // mSetResultAfterSave = true in onEvent
                mSetResultAfterSave = false;
                LogUtil.i(TAG, "<initWhenHaveService> save record when run from other ap");
                mService.doSaveRecord(mButtonUpdater);
                mSavingRecordFileFromMms = true;
            } else {
                mService.reset();
            }
        }
        restoreRecordParamsSettings();
        mHandler.sendEmptyMessage(mService.getCurrentState());
        // do action that need to bo in onActivityResult
        if (RECORD.equals(mDoWhat)) {
            onClickRecordButton();
        } else if (PLAY.equals(mDoWhat)) {
            mService.playFile(mDoWhatFilePath);
        }
        mDoWhat = null;
        mDoWhatFilePath = null;
        LogUtil.i(TAG, "<initWhenHaveService> end");
    }

    /*
     * Whenever the UI is re-created (due f.ex. to orientation change) we have
     * to reinitialize references to the views.
     */
    private void initResourceRefs() {
        LogUtil.i(TAG, "<initResourceRefs> start");
        initResourceRefsWhenNoService();
        /**
         * set related property according to if SoundRecorder is started by
         * launcher
         */
        if (mRunFromLauncher) {
            mPlayButton.setOnClickListener(this);
            mFileListButton.setOnClickListener(this);
            mPauseRecordingButton.setOnClickListener(this);
        } else {
            mPlayButton.setVisibility(View.GONE);
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.GONE);
        }

        mRecordButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mAcceptButton.setOnClickListener(this);
        mDiscardButton.setOnClickListener(this);
        setTitle(getResources().getString(R.string.app_name));
        mVUMeter.setRecorder(mService.getRecorder());
        if (mResumeNeedRefresh) {
            setTimerTextView(true);
            mResumeNeedRefresh = false;
        }
        LogUtil.i(TAG, "<initResourceRefs> end");
    }

    /**
     * show dialog use DialogFragment.
     *
     * @param id   the flag of dialog
     * @param args the parameters of create dialog
     *             <p>
     *             M: use DialogFragment to show dialog, for showDialog() is
     *             deprecated in current version
     */
    private void showDialogFragment(int id, Bundle args) {
        LogUtil.i(TAG, "<showDialogFragment> start");
        DialogFragment newFragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (id) {
            case DIALOG_SELECT_FORMAT:
                removeOldFragmentByTag(DIALOG_TAG_SELECT_FORMAT);
                newFragment = SelectDialogFragment.newInstance(RecordParamsSetting
                                .getFormatStringArray(this), null,
                        R.string.select_voice_quality, true, mSelectedFormat, null);
                ((SelectDialogFragment) newFragment).setOnClickListener(mSelectFormatListener);
                newFragment.show(fragmentManager, DIALOG_TAG_SELECT_FORMAT);
                LogUtil.i(TAG, "<showDialogFragment> show select format dialog");
                break;
            case DIALOG_SELECT_MODE:
                removeOldFragmentByTag(DIALOG_TAG_SELECT_MODE);
                newFragment = SelectDialogFragment.newInstance(RecordParamsSetting
                                .getModeStringIDArray(), null, R.string.select_recording_mode, true,
                        mSelectedMode, null);
                ((SelectDialogFragment) newFragment).setOnClickListener(mSelectModeListener);
                newFragment.show(fragmentManager, DIALOG_TAG_SELECT_MODE);
                LogUtil.i(TAG, "<showDialogFragment> show select mode dialog");
                break;
            case DIALOG_SELECT_EFFECT:
                removeOldFragmentByTag(DIALOG_TAG_SELECT_EFFECT);
                newFragment = SelectDialogFragment.newInstance(RecordParamsSetting
                                .getEffectStringIDArray(), null, R.string.select_recording_effect, false, 0,
                        mSelectEffectArray);
                ((SelectDialogFragment) newFragment).setOnClickListener(mSelectEffectOkListener);
                ((SelectDialogFragment) newFragment)
                        .setOnMultiChoiceListener(mSelectEffectMultiChoiceClickListener);
                newFragment.show(fragmentManager, DIALOG_TAG_SELECT_EFFECT);
                break;
            default:
                break;
        }
        fragmentManager.executePendingTransactions();
        LogUtil.i(TAG, "<showDialogFragment> end");
    }

    /**
     * remove old DialogFragment.
     *
     * @param tag the tag of DialogFragment to be removed
     */
    private void removeOldFragmentByTag(String tag) {
        LogUtil.i(TAG, "<removeOldFragmentByTag> start");
        FragmentManager fragmentManager = getSupportFragmentManager();
        DialogFragment oldFragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        LogUtil.i(TAG, "<removeOldFragmentByTag> oldFragment = " + oldFragment);
        if (null != oldFragment) {
            oldFragment.dismissAllowingStateLoss();
            LogUtil.i(TAG, "<removeOldFragmentByTag> remove oldFragment success");
        }
        LogUtil.i(TAG, "<removeOldFragmentByTag> end");
    }

    /**
     * set current record mode when user select an item in SelectDialogFragment.
     *
     * @param which mode to be set
     */
    public void setSelectedMode(int which) {
        mSelectedMode = RecordParamsSetting.getSelectMode(which);
        if (null != mService) {
            mService.setSelectedMode(mSelectedMode);
        }
        LogUtil.i(TAG, "<setSelectedMode> mSelectedMode = " + mSelectedMode);
    }

    /**
     * set current record format when user select an item in
     * SelectDialogFragment.
     *
     * @param which which format has selected
     */
    public void setSelectedFormat(int which) {
        mSelectedFormat = RecordParamsSetting.getSelectFormat(which);
        if (null != mService) {
            mService.setSelectedFormat(mSelectedFormat);
        }
        LogUtil.i(TAG, "<setSelectedFormat> mSelectedFormat = " + mSelectedFormat);
    }

    /**
     * reset the UI to initial state when mService is not available, only
     * used in onResume.
     */
    private void resetUi() {
        initResourceRefsWhenNoService();
        disableButton();
        setTitle(getResources().getString(R.string.app_name));
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mPlayButton.setVisibility(View.GONE);
        mStopButton.setEnabled(false);
        mStopButton.setFocusable(false);
        mStopButton.setVisibility(View.GONE);
        if (mRunFromLauncher) {
            mFileListButton.setVisibility(View.VISIBLE);
            mFileListButton.setEnabled(true);
            mFileListButton.setFocusable(true);
        } else {
            mFileListButton.setVisibility(View.GONE);
            mStopButton.setVisibility(View.VISIBLE);
            mStopButton.setEnabled(false);
            mStopButton.setFocusable(false);
        }
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.INVISIBLE);
        mFirstLine.setVisibility(View.GONE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mVUMeter.mCurrentAngle = 0;
        mStateProgressBar.setVisibility(View.INVISIBLE);
        /** Avoid fresh the timer view if current is recording state.@{**/
        if (mService == null ||
                (mService.getCurrentState() != SoundRecorderService.STATE_RECORDING)) {
            setTimerTextView(true);
        }
    }

    private void initResourceRefsWhenNoService() {
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        mStopButton = (ImageButton) findViewById(R.id.stopButton);
        mPlayButton = (ImageButton) findViewById(R.id.playButton);
        mFileListButton = (ImageButton) findViewById(R.id.fileListButton);
        mPauseRecordingButton = (ImageButton) findViewById(R.id.pauseRecordingButton);
        mButtonParent = (LinearLayout) findViewById(R.id.buttonParent);
        mRecordingStateImageView = (ImageView) findViewById(R.id.stateLED);
        mRemainingTimeTextView = (TextView) findViewById(R.id.stateMessage1);
        mStateTextView = (TextView) findViewById(R.id.stateMessage2);
        mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);
        mTimerTextView = (TextView) findViewById(R.id.timerView);
        mPlayingStateImageView = (ImageView) findViewById(R.id.currState);
        mRecordingFileNameTextView = (TextView) findViewById(R.id.recordingFileName);
        mExitButtons = (LinearLayout) findViewById(R.id.exitButtons);
        mVUMeter = (VUMeter) findViewById(R.id.uvMeter);
        mTimerFormat = getResources().getString(R.string.timer_format);
        mAcceptButton = (Button) findViewById(R.id.acceptButton);
        mDiscardButton = (Button) findViewById(R.id.discardButton);
        mFirstLine = (ImageView) findViewById(R.id.firstLine);
        mSecondLine = (ImageView) findViewById(R.id.secondLine);
        mFrameLayout = (FrameLayout) findViewById(R.id.frameLayout);
    }

    /**
     * Update UI on idle state.
     */
    private void updateUiOnIdleState() {
        LogUtil.i(TAG, "<updateUiOnIdleState> start");
        boolean isCurrentFileWaitToSave = mService.isCurrentFileWaitToSave();
        int time = 0;
        if (mFileFromList) {
            mFileFromList = false;
        } else {
            time = (int) mService.getCurrentFileDurationInSecond();
        }
        if (mIsRecordStarting) {
            time = 0;
        }
        String timerString = String.format(mTimerFormat, time / TIME_BASE, time % TIME_BASE);
        setTimerViewTextSize(time);
        LogUtil.i(TAG, "<updateUiOnIdleState> time = " + timerString);
        mTimerTextView.setText(timerString);

        String currentFilePath = mService.getCurrentFilePath();
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mStateProgressBar.setProgress(0);
        mStateProgressBar.setVisibility(View.INVISIBLE);
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.VISIBLE);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mRecordButton.requestFocus();
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setEnabled(false);
        mStopButton.setVisibility(View.GONE);
        if (null == currentFilePath) {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.frameLayoutGrayColor));
            mPlayButton.setVisibility(View.GONE);
            if (mRunFromLauncher) {
                mFileListButton.setVisibility(View.VISIBLE);
                mFileListButton.setEnabled(true);
                mFileListButton.setFocusable(true);
            } else {
                mFileListButton.setVisibility(View.GONE);
                mStopButton.setVisibility(View.VISIBLE);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);
            }
            mRemainingTimeTextView.setVisibility(View.INVISIBLE);
            mPlayingStateImageView.setVisibility(View.GONE);
            mRecordingFileNameTextView.setVisibility(View.INVISIBLE);
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
            mVUMeter.setVisibility(View.VISIBLE);
            mVUMeter.mCurrentAngle = 0;
        } else {
            if (mRunFromLauncher) {
                mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
                mPlayButton.setVisibility(View.VISIBLE);
                mPlayButton.setEnabled(true);
                mPlayButton.setFocusable(true);
                mPlayButton.setImageResource(R.drawable.play);
            }
            mRemainingTimeTextView.setVisibility(View.INVISIBLE);
            mPlayingStateImageView.setImageResource(R.drawable.stop);
            mPlayingStateImageView.setVisibility(View.VISIBLE);
            mRecordingFileNameTextView.setVisibility(View.VISIBLE);
            mExitButtons.setVisibility(View.INVISIBLE);
            mVUMeter.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mFileListButton.setVisibility(View.VISIBLE);
            if (isCurrentFileWaitToSave && mService.getCurrentState() ==
                    SoundRecorderService.STATE_IDLE) {
                mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
                mSecondLine.setVisibility(View.INVISIBLE);
                mFirstLine.setVisibility(View.VISIBLE);
                mExitButtons.setVisibility(View.VISIBLE);
                mAcceptButton.setEnabled(true);
                mDiscardButton.setEnabled(true);
                mStopButton.setVisibility(View.VISIBLE);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);
                mFileListButton.setVisibility(View.GONE);
            } else {
                mFrameLayout.setBackgroundColor(getResources().getColor(
                        R.color.frameLayoutGrayColor));
                mSecondLine.setVisibility(View.VISIBLE);
                mFileListButton.setEnabled(true);
            }
        }
        mIsButtonDisabled = false;
        LogUtil.i(TAG, "<updateUiOnIdleState> end");
    }

    /**
     * Update UI on success state.
     */
    private void updateUiOnSaveSuccessState() {
        LogUtil.i(TAG, "<updateUiOnSaveSuccessState> start");
        updateUiOnIdleState();
        LogUtil.i(TAG, "<updateUiOnSaveSuccessState> end");
    }

    /**
     * Update UI on pause playing state.
     */
    private void updateUiOnPausePlayingState() {
        LogUtil.i(TAG, "<updateUiOnPausePlayingState> start");
        mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mPlayButton.setVisibility(View.VISIBLE);
        mPlayButton.setEnabled(true);
        mPlayButton.setFocusable(true);
        mPlayButton.setImageResource(R.drawable.play);
        mStopButton.setVisibility(View.VISIBLE);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setImageResource(R.drawable.pause);
        mPlayingStateImageView.setVisibility(View.VISIBLE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        boolean isCurrentFileWaitToSave = false;
        if (null != mService) {
            isCurrentFileWaitToSave = mService.isCurrentFileWaitToSave();
        }
        if (!isCurrentFileWaitToSave) {
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
        } else {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
            mExitButtons.setVisibility(View.VISIBLE);
            mAcceptButton.setEnabled(true);
            mDiscardButton.setEnabled(true);
            mFirstLine.setVisibility(View.VISIBLE);
            mSecondLine.setVisibility(View.INVISIBLE);
        }
        setTimerTextView(false);
        mStateProgressBar.setVisibility(View.VISIBLE);
        mStateProgressBar.setProgress((int) (100
                * mService.getCurrentProgressInMillSecond()
                / mService.getCurrentFileDurationInMillSecond()));
        mVUMeter.setVisibility(View.INVISIBLE);
        mIsButtonDisabled = false;
        LogUtil.i(TAG, "<updateUiOnPausePlayingState> end");
    }

    /**
     * Update UI on recording state.
     */
    private void updateUiOnRecordingState() {
        LogUtil.i(TAG, "<updateUiOnRecordingState> start");
        mFrameLayout.setBackgroundColor(getResources().getColor(R.color.frameLayoutGrayColor));
        Resources res = getResources();
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.GONE);
        mPlayButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        if (mRunFromLauncher) {
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.VISIBLE);
            mPauseRecordingButton.setEnabled(true);
            mPauseRecordingButton.setFocusable(true);
            mPauseRecordingButton.setSoundEffectsEnabled(false);
        } else {
            mRecordButton.setVisibility(View.VISIBLE);
            mRecordButton.setEnabled(false);
            mRecordButton.setFocusable(false);
            mRecordButton.setSoundEffectsEnabled(true);
            mFileListButton.setVisibility(View.GONE);
            mPauseRecordingButton.setVisibility(View.GONE);
        }
        mRecordingStateImageView.setVisibility(View.VISIBLE);
        mRecordingStateImageView.setImageResource(R.drawable.recording_led);
        mStateTextView.setVisibility(View.VISIBLE);
        mStateTextView.setText(res.getString(R.string.recording));
        mRemainingTimeTextView.setText(EMPTY);
        mRemainingTimeTextView.setVisibility(View.VISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mFirstLine.setVisibility(View.INVISIBLE);
        mSecondLine.setVisibility(View.VISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mStateProgressBar.setVisibility(View.INVISIBLE);
        int remainingTime = (int) mService.getRemainingTime();
        boolean isUpdateRemainingTimerView =
                mHasFileSizeLimitation ? (remainingTime < MMS_FILE_LIMIT)
                        : (remainingTime < TIME_NINE_MIN);
        if ((remainingTime > 0) && isUpdateRemainingTimerView) {
            updateRemainingTimerView(remainingTime);
        }
        setTimerTextView(false);
        mIsButtonDisabled = false;
        LogUtil.i(TAG, "<updateUiOnRecordingState> end");
    }

    /**
     * Update UI on pause Recording state.
     */
    private void updateUiOnPauseRecordingState() {
        LogUtil.i(TAG, "<updateUiOnPauseRecordingState> start");
        Resources res = getResources();
        mButtonParent.setWeightSum(TWO_BUTTON_WEIGHT_SUM);
        mRecordButton.setVisibility(View.VISIBLE);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(false);
        mPlayButton.setVisibility(View.GONE);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRecordingStateImageView.setVisibility(View.VISIBLE);
        mRecordingStateImageView.setImageResource(R.drawable.idle_led);
        mStateTextView.setVisibility(View.VISIBLE);
        mStateTextView.setText(res.getString(R.string.recording_paused));
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.GONE);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        mExitButtons.setVisibility(View.INVISIBLE);
        mFirstLine.setVisibility(View.INVISIBLE);
        mSecondLine.setVisibility(View.VISIBLE);
        mVUMeter.setVisibility(View.VISIBLE);
        mVUMeter.mCurrentAngle = 0;
        mStateProgressBar.setVisibility(View.INVISIBLE);
        setTimerTextView(false);
        mIsButtonDisabled = false;
        LogUtil.i(TAG, "<updateUiOnPauseRecordingState> end");
    }

    /**
     * Update UI on playing state.
     */
    private void updateUiOnPlayingState() {
        LogUtil.i(TAG, "<updateUiOnPlayingState> start");
        mButtonParent.setWeightSum(THREE_BUTTON_WEIGHT_SUM);
        mRecordButton.setEnabled(true);
        mRecordButton.setFocusable(true);
        mRecordButton.setSoundEffectsEnabled(true);
        mPlayButton.setVisibility(View.VISIBLE);
        mPlayButton.setEnabled(true);
        mPlayButton.setFocusable(true);
        mPlayButton.setImageResource(R.drawable.pause);
        mFileListButton.setVisibility(View.GONE);
        mPauseRecordingButton.setVisibility(View.GONE);
        mPauseRecordingButton.setSoundEffectsEnabled(false);
        mStopButton.setVisibility(View.VISIBLE);
        mStopButton.setEnabled(true);
        mStopButton.setFocusable(true);
        mRecordingStateImageView.setVisibility(View.INVISIBLE);
        mStateTextView.setVisibility(View.INVISIBLE);
        mRemainingTimeTextView.setVisibility(View.INVISIBLE);
        mPlayingStateImageView.setVisibility(View.VISIBLE);
        mPlayingStateImageView.setImageResource(R.drawable.play);
        mRecordingFileNameTextView.setVisibility(View.VISIBLE);
        if (!mService.isCurrentFileWaitToSave()) {
            mExitButtons.setVisibility(View.INVISIBLE);
            mFirstLine.setVisibility(View.INVISIBLE);
            mSecondLine.setVisibility(View.VISIBLE);
        } else {
            mFrameLayout.setBackgroundColor(getResources().getColor(R.color.blackColor));
            mExitButtons.setVisibility(View.VISIBLE);
            mAcceptButton.setEnabled(true);
            mDiscardButton.setEnabled(true);
            mFirstLine.setVisibility(View.VISIBLE);
            mSecondLine.setVisibility(View.INVISIBLE);
        }
        mVUMeter.setVisibility(View.INVISIBLE);
        mStateProgressBar.setVisibility(View.VISIBLE);
        setTimerTextView(true);
        mIsButtonDisabled = false;
        LogUtil.i(TAG, "<updateUiOnPlayingState> end");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.i(TAG, "<onResume> start mRunFromLauncher = " + mRunFromLauncher + ", Activity = "
                + this.toString());
        mOnSaveInstanceStateHasRun = false;

        if (mService == null) {
            disableButton();
            // start service
            LogUtil.i(TAG, "<onResume> start service");
            if (null == startService(new Intent(this, SoundRecorderService.class))) {
                LogUtil.e(TAG, "<onResume> fail to start service");
                finish();
                return;
            }

            // bind service
            LogUtil.i(TAG, "<onResume> bind service");
            if (!bindService(new Intent(this, SoundRecorderService.class),
                    mServiceConnection, BIND_AUTO_CREATE)) {
                LogUtil.e(TAG, "<onResume> fail to bind service");
                finish();
                return;
            }

            // reset ui to initial state, or else the UI may be abnormal before service not bind
            if (mResumeNeedRefresh) {
                resetUi();
            }
        } else {
            // M: when switch SoundRecorder and RecordingFileList quickly, it's
            // possible that onStop was not been called,
            // but onResume is called, in this case, mService has not been
            // unbind, so mService != null
            // but we still should do some initial operation, such as play
            // recording file which select from RecordingFileList
            initWhenHaveService();
        }
        if (mDoRecordWhenResume) {
            onClickRecordButton();
            mDoRecordWhenResume = false;
        }
        LogUtil.i(TAG, "<onResume> end");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (!handleIntent(getIntent())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
    }

    @Override
    public void onBackPressed() {
        LogUtil.i(TAG, "<onBackPressed> start, Activity = " + this.toString());
        mBackPressed = true;
        if (!mRunFromLauncher) {
            if (mService != null) {
                mService.doStop(mButtonUpdater);
                if (mService.isCurrentFileWaitToSave()) {
                    LogUtil.i(TAG, "<onBackPressed> mService.saveRecord()");
                    mService.doSaveRecord(mButtonUpdater);
                } else {
                    // M: if not call saveRecord, we finish activity by ourself
                    finish();
                }
            } else {
                // M: if not call saveRecord, we finish activity by ourself
                finish();
            }
        } else {
            // M: if run from launcher, we do not run other operation when back
            // key pressed
            if (null != mService) {
                mService.storeRecordParamsSettings();
            }
            super.onBackPressed();
        }
        LogUtil.i(TAG, "<onBackPressed> end");
    }

    /**
     * add process of view focus, and save configuration change state when
     * SoundRecorder is started by other application
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LogUtil.i(TAG, "<onConfigurationChanged> start");
        View viewFocus = this.getCurrentFocus();
        int viewId = -1;
        if (null != viewFocus) {
            viewId = viewFocus.getId();
        }
        setContentView(R.layout.activity_sound_recorder);

        if (null != mService) {
            initResourceRefs();
            if (!mService.isCurrentFileWaitToSave()) {
                mExitButtons.setVisibility(View.INVISIBLE);
                mFirstLine.setVisibility(View.INVISIBLE);
            }
            mHandler.sendEmptyMessage(mService.getCurrentState());
            mService.storeRecordParamsSettings();
        } else {
            resetUi();
            disableButton();
        }

        if (viewId >= 0) {
            View view = findViewById(viewId);
            if (null != view) {
                view.setFocusable(true);
                view.requestFocus();
            }
        }
        LogUtil.i(TAG, "<onConfigurationChanged> end");
    }

    /**
     * do record or play operation after press record
     * or press one record item in RecordingFileList
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.i(TAG, "<onActivityResult> start");
        if (RESULT_OK != resultCode) {
            LogUtil.i(TAG, "<onActivityResult> resultCode != RESULT_OK, return");
            return;
        }
        if ((null != mService) && (null != mFileListButton)) {
            mFileListButton.setEnabled(true);
        }
        Intent intent = data;
        Bundle bundle = intent.getExtras();
        if (null == bundle) {
            LogUtil.i(TAG, "<onActivityResult> bundle == null, return");
            return;
        }
        mDoWhat = bundle.getString(DOWHAT);
        if (null != mDoWhat) {
            if (mDoWhat.equals(PLAY)) {
                if ((null != intent.getExtras()) && (null != intent.getExtras().getString(PATH))) {
                    mDoWhatFilePath = intent.getExtras().getString(PATH);
                    mFileFromList = true;
                }
            }
        }
        // later, after mService connected, play/record
        LogUtil.i(TAG, "<onActivityResult> end");
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        LogUtil.i(TAG, "<onSaveInstanceState> start");
        mOnSaveInstanceStateHasRun = true;
        if (null != mService) {
            mService.storeRecordParamsSettings();
        }
        LogUtil.i(TAG, "<onSaveInstanceState> end");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        LogUtil.i(TAG, "<onRestoreInstanceState> start");
        restoreDialogFragment();
        restoreRecordParamsSettings();
        LogUtil.i(TAG, "<onRestoreInstanceState> end");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean mShowPermission = true;
        if (permissions.length == 0) {
            return;
        }
        if (requestCode == PERMISSION_RECORD_AUDIO) {
            boolean granted = true;
            for (int counter = 0; counter < permissions.length; counter++) {
                granted = granted && (grantResults[counter] == PackageManager.PERMISSION_GRANTED);
                LogUtil.i(TAG, "<onRequestPermissionsResult> " + grantResults[counter]);
                if (grantResults[counter] != PackageManager.PERMISSION_GRANTED) {
                    mShowPermission = mShowPermission &&
                            shouldShowRequestPermissionRationale(permissions[counter]);
                }
                LogUtil.i(TAG, "<onRequestPermissionsResult1>" + granted + mShowPermission);
            }
            if (granted == true) {
                if (mRunFromLauncher) {
                    onClickRecordButton();
                } else {
                    mDoRecordWhenResume = true;
                }
            } else {
                if (mShowPermission == false) {
                    //SoundRecorderUtils.getToast(SoundRecorder.this,
                    //        com.mediatek.internal.R.string.denied_required_permission);
                    return;
                }
            }
        } else if (requestCode == PERMISSION_READ_STORAGE_LIST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClickFileListButton();
            } else {
                if (!shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    //SoundRecorderUtils.getToast(
                    //    SoundRecorder.this,
                    //    com.mediatek.internal.R.string.denied_required_permission);
                    return;
                }
            }
        } else if (requestCode == PERMISSION_WRITE_STORAGE_LIST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onClickAcceptButton();
            } else {
                if (!shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //SoundRecorderUtils.getToast(
                    //SoundRecorder.this,
                    //com.mediatek.internal.R.string.denied_required_permission);
                    return;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        LogUtil.i(TAG, "<onCreateOptionsMenu> begin");
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = new MenuInflater(getApplicationContext());
        menuInflater.inflate(R.menu.menu_sound_recorder, menu);
        mMenu = menu;
        LogUtil.i(TAG, "<onCreateOptionsMenu> end");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        LogUtil.i(TAG, "<onPrepareOptionsMenu> start");
        super.onPrepareOptionsMenu(menu);
        mMenu = menu;
        updateOptionsMenu(true);
        LogUtil.i(TAG, "<onPrepareOptionsMenu> end");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.record_format == id) {
            showDialogFragment(DIALOG_SELECT_FORMAT, null);
        } else if (R.id.record_mode == id) {
            showDialogFragment(DIALOG_SELECT_MODE, null);
        } else if (R.id.record_effect == id) {
            mSelectEffectArrayTemp = mSelectEffectArray.clone();
            showDialogFragment(DIALOG_SELECT_EFFECT, null);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        // avoid to response button click event when
        // activity is paused/stopped/destroy.
        if (isFinishing()) {
            return;
        }
        if (!v.isEnabled()) {
            return;
        }
        LogUtil.i(TAG, "<onClick> Activity = " + this.toString());
        switch (v.getId()) {
            case R.id.recordButton:
                LogUtil.i(TAG, "<onClick> recordButton");
                onClickRecordButton();
                break;
            case R.id.playButton:
                LogUtil.i(TAG, "<onClick> playButton");
                onClickPlayButton();
                break;
            case R.id.stopButton:
                LogUtil.i(TAG, "<onClick> stopButton");
                onClickStopButton();
                break;
            case R.id.acceptButton:
                LogUtil.i(TAG, "<onClick> acceptButton");
                onClickAcceptButton();
                break;
            case R.id.discardButton:
                LogUtil.i(TAG, "<onClick> discardButton");
                onClickDiscardButton();
                break;
            case R.id.fileListButton:
                onClickFileListButton();
                break;
            case R.id.pauseRecordingButton:
                LogUtil.i(TAG, "<onClick> pauseRecordingButton");
                onClickPauseRecordingButton();
                break;
            default:
                break;
        }
    }

    /**
     * process after click record button.
     */
    void onClickRecordButton() {
        if (OptionsUtil.isRunningInEmulator()) {
            LogUtil.d(TAG, "for special action for emulator load, do nothing...");
            return;
        }
        boolean recordAudioPermission = PermissionUtil.isGranted(this, Manifest.permission.RECORD_AUDIO);
        boolean readExtStoragePermission = PermissionUtil.isGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean writeExtStoragePermission = PermissionUtil.isGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!(recordAudioPermission && readExtStoragePermission && writeExtStoragePermission)) {
            List<String> mPermissionStrings = new ArrayList<String>();
            boolean mRequest = false;
            LogUtil.d(TAG, "<onClickRecordButton> " + recordAudioPermission
                    + readExtStoragePermission + writeExtStoragePermission);
            if (!writeExtStoragePermission) {
                mPermissionStrings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                mRequest = true;
            }
            if (!readExtStoragePermission) {
                mPermissionStrings.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                mRequest = true;
            }
            if (!recordAudioPermission) {
                mPermissionStrings.add(Manifest.permission.RECORD_AUDIO);
                mRequest = true;
            }
            if (mRequest == true) {
                String[] mPermissionList = new String[mPermissionStrings.size()];
                mPermissionList = mPermissionStrings.toArray(mPermissionList);
                requestPermissions(mPermissionList, PERMISSION_RECORD_AUDIO);
                return;
            }
        }

        if (null != mService) {
            mIsRecordStarting = true;
            mService.startRecordingAsync(
                    RecordParamsSetting.getRecordParams(mRequestedType, mSelectedFormat,
                            mSelectedMode, mSelectEffectArray, this),
                    (int) mMaxFileSize, mButtonUpdater);
        }
        long mEndRecordingTime = System.currentTimeMillis();
        LogUtil.i(TAG, "[Performance test][SoundRecorder] recording end [" + mEndRecordingTime + "]");
    }

    /**
     * process after click play button.
     */
    void onClickPlayButton() {
        if (!isFinishing() && null != mService) {
            mService.doPlayRecord(mButtonUpdater);
        }
    }

    /**
     * process after click stop button.
     */
    void onClickStopButton() {
        if (null == mService) {
            long mEndStopTime = System.currentTimeMillis();
            LogUtil.i(TAG, "[Performance test][SoundRecorder] recording stop end [" + mEndStopTime
                    + "]");
            return;
        }
        mService.doStop(mButtonUpdater);
        long mEndStopTime = System.currentTimeMillis();
        LogUtil.i(TAG, "[Performance test][SoundRecorder] recording stop end [" + mEndStopTime + "]");
    }

    /**
     * process after click accept button.
     */
    void onClickAcceptButton() {
        if (null == mService) {
            return;
        }
        if (!PermissionUtil.isGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            LogUtil.i(TAG, "<onClickAcceptButton> Need storage permission");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_STORAGE_LIST);
            return;
        }
        mService.doSaveRecord(mButtonUpdater);
    }

    /**
     * process after click discard button.
     */
    void onClickDiscardButton() {
        if (mService != null) {
            mService.doDiscardRecord(mButtonUpdater);
        }
        mVUMeter.mCurrentAngle = 0;
        mVUMeter.invalidate();
    }

    /**
     * process after click file list button.
     */
    void onClickFileListButton() {
        if (!PermissionUtil.isGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            LogUtil.i(TAG, "<onClickRecordButton> Need storage permission");
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_READ_STORAGE_LIST);
            return;
        }
        disableButton();
        if (null != mService) {
            LogUtil.i(TAG, "<onClickFileListButton> mService.resetAsync()");
            mService.resetAsync();
        }
        Intent mIntent = new Intent();
        mIntent.setClass(this, RecordingFileListActivity.class);
        startActivityForResult(mIntent, REQUEST_FILE_LIST);
    }

    /**
     * process after click pause recording button.
     */
    void onClickPauseRecordingButton() {
        if (null != mService) {
            mService.doPause(mButtonUpdater);
        }
    }

    @Override
    public void onStateChanged(int state) {
        LogUtil.i(TAG, "<onStateChanged> change to " + state);
        if (!mRunFromLauncher) {
            if (state == SoundRecorderService.STATE_RECORDING) {
                acquireWakeLock();
            } else {
                releaseWakeLock();
            }
        }
        mHandler.removeMessages(state);
        mHandler.sendEmptyMessage(state);
    }

    @Override
    public void onError(int error) {
        LogUtil.i(TAG, "<onError> errorCode = " + error);
        // M: if OnSaveInstanceState has run, we do not show Dialogfragment now,
        // or else FragmentManager will throw IllegalStateException
        if (!mOnSaveInstanceStateHasRun) {
            Bundle bundle = new Bundle(1);
            bundle.putInt(ERROR_CODE, error);
            Message msg = mHandler.obtainMessage(SoundRecorderService.STATE_ERROR);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    protected void onPause() {
        LogUtil.i(TAG, "<onPause> start, Activity =" + this.toString());
        if (!mBackPressed && mService != null && !mRunFromLauncher) {
            if (mService.getCurrentState() == SoundRecorderService.STATE_RECORDING) {
                mService.doStop(mButtonUpdater);
            }
            if (mService.isCurrentFileWaitToSave()) {
                LogUtil.i(TAG, "<onPause> mService.saveRecord()");
                mService.saveRecordAsync();
            }
        }
        mBackPressed = false;
        LogUtil.i(TAG, "<onPause> end");
        super.onPause();
    }

    @Override
    protected void onStop() {
        LogUtil.i(TAG, "<onStop> start, Activity = " + this.toString());
        if (mRunFromLauncher && mService != null) {

            boolean stopService = (mService.getCurrentState() == SoundRecorderService.STATE_IDLE)
                    && !mService.isCurrentFileWaitToSave();

            // M: if another instance of soundrecorder has been resume,
            // the listener of service has changed to another instance, so we
            // cannot call setAllListenerSelf
            boolean isListener = mService.isListener(this);
            LogUtil.i(TAG, "<onStop> isListener = " + isListener);
            if (isListener) {
                // set listener of service as default,
                // so when error occurs, service can show error info in toast
                mService.setAllListenerSelf();
            }

            LogUtil.i(TAG, "<onStop> unbind service");
            unbindService(mServiceConnection);

            mIsStopService = stopService && isListener;
            mService = null;
        }
        hideStorageHint();
        LogUtil.i(TAG, "<onStop> end");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtil.i(TAG, "<onDestroy> start, Activity = " + this.toString());
        if (mRunFromLauncher && mIsStopService) {
            LogUtil.i(TAG, "<onDestroy> stop service");
            stopService(new Intent(this, SoundRecorderService.class));
        }
        if (!mRunFromLauncher) {
            releaseWakeLock();
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        LogUtil.i(TAG, "<onDestroy> end");
        super.onDestroy();
    }

    /**
     * Update the big MM:SS timer. If we are in play back, also update the
     * progress bar.
     *
     * @param time time to be updated
     */
    @Override
    public void updateTimerView(int time) {
        LogUtil.i(TAG, "<updateTimerView> start time = " + time);
        int state = mService.getCurrentState();
        // update progress bar
        if (SoundRecorderService.STATE_PLAYING == state) {
            long fileDuration = mService.getCurrentFileDurationInMillSecond();
            LogUtil.i(TAG, "<updateTimerView> fileDuration = " + fileDuration);
            if (fileDuration > ONE_SECOND) {
                long progress = mService.getCurrentProgressInMillSecond();
                LogUtil.i(TAG, "<updateTimerView> progress = " + (fileDuration - progress));
                if (fileDuration - progress < SoundRecorderService.WAIT_TIME) {
                    mStateProgressBar.setProgress(DONE);
                } else {
                    mStateProgressBar.setProgress((int) (100 * progress / fileDuration));
                }
            } else {
                mStateProgressBar.setProgress(DONE);
            }
        }
        // update timer
        setTimerTextView(time);
        // update remaining time
        if (SoundRecorderService.STATE_RECORDING == mService.getCurrentState()) {
            int remainingTime = (int) mService.getRemainingTime();
            if (mService.isStorageLower()) {
                showStorageHint(getString(R.string.storage_low));
            } else {
                hideStorageHint();
            }
            boolean isUpdateRemainingTimerView =
                    mHasFileSizeLimitation ? (remainingTime < MMS_FILE_LIMIT)
                            : (remainingTime < TIME_NINE_MIN);
            if ((remainingTime > 0) && isUpdateRemainingTimerView) {
                updateRemainingTimerView(remainingTime);
            }
        }
        LogUtil.i(TAG, "<updateTimerView> end");
    }

    /**
     * set text size bigger if time >= 100.
     *
     * @param time time in minutes should be shown
     */
    private void setTimerViewTextSize(int time) {
        /** set text size bigger if >= 100 */
        final int textSizeChangeBoundary = 100;
        float scale = getResources().getConfiguration().fontScale;
        float textSize;
        if ((time / TIME_BASE) >= textSizeChangeBoundary) {
            textSize = getResources().getDimension(R.dimen.timerView_TextSize_Small);
        } else {
            textSize = getResources().getDimension(R.dimen.timerView_TextSize_Big);
        }
        if (scale > 1) {
            textSize = textSize / scale;
        }
        mTimerTextView.setTextSize(textSize);
    }

    private void restoreDialogFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(DIALOG_TAG_SELECT_FORMAT);
        if (null != fragment) {
            ((SelectDialogFragment) fragment).setOnClickListener(mSelectFormatListener);
        }

        fragment = fragmentManager.findFragmentByTag(DIALOG_TAG_SELECT_MODE);
        if (null != fragment) {
            ((SelectDialogFragment) fragment).setOnClickListener(mSelectModeListener);
        }

        fragment = fragmentManager.findFragmentByTag(DIALOG_TAG_SELECT_EFFECT);
        if (null != fragment) {
            ((SelectDialogFragment) fragment)
                    .setOnMultiChoiceListener(mSelectEffectMultiChoiceClickListener);
            ((SelectDialogFragment) fragment).setOnClickListener(mSelectEffectOkListener);
        }
    }

    private void restoreRecordParamsSettings() {
        LogUtil.i(TAG, "<restoreRecordParamsSettings> ");
        if (mSelectedFormat != -1) {
            if (null != mService) {
                mService.setSelectedFormat(mSelectedFormat);
                mService.setSelectedMode(mSelectedMode);
                mService.setSelectEffectArray(mSelectEffectArray);
                mService.setSelectEffectArrayTmp(mSelectEffectArrayTemp);
            }
            LogUtil.i(TAG, "<restoreRecordParamsSettings> selectedFormat return ");
            return;
        }
        if (null == mPrefs) {
            mPrefs = getSharedPreferences(SOUND_RECORDER_DATA, 0);
        }
        int defaultRecordingLevel =
                RecordParamsSetting.getDefaultRecordingLevel(RecordParamsSetting.FORMAT_HIGH);
        mSelectedFormat = mPrefs.getInt(SoundRecorderService.SELECTED_RECORDING_FORMAT,
                defaultRecordingLevel);
        if (mSelectedFormat < 0 || mSelectedFormat > RecordParamsSetting.getQualityLevelNumber()) {
            mSelectedFormat = defaultRecordingLevel;
        }
        mSelectedMode = mPrefs
                .getInt(SoundRecorderService.SELECTED_RECORDING_MODE,
                        RecordParamsSetting.MODE_NORMAL);
        if (mSelectedMode < 0) {
            mSelectedMode = RecordParamsSetting.MODE_NORMAL;
        }
        mSelectEffectArray[RecordParamsSetting.EFFECT_AEC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AEC, false);
        mSelectEffectArray[RecordParamsSetting.EFFECT_AGC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AGC, false);
        mSelectEffectArray[RecordParamsSetting.EFFECT_NS] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_NS, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_AEC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AEC_TMP, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_AGC] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_AGC_TMP, false);
        mSelectEffectArrayTemp[RecordParamsSetting.EFFECT_NS] = mPrefs.getBoolean(
                SoundRecorderService.SELECTED_RECORDING_EFFECT_NS_TMP, false);
        if (null != mService) {
            mService.setSelectedFormat(mSelectedFormat);
            mService.setSelectedMode(mSelectedMode);
            mService.setSelectEffectArray(mSelectEffectArray);
            mService.setSelectEffectArrayTmp(mSelectEffectArrayTemp);
        }
        LogUtil.i(TAG, "mSelectedFormat is:"
                + mSelectedFormat + "; mSelectedMode is:" + mSelectedMode);
    }

    private void updateRemainingTimerView(int second) {
        String timeString = "";
        if (second < 0) {
            mRemainingTimeTextView.setText(NULL_STRING);
        } else if (second < TIME_BASE) {
            timeString = String.format(getString(R.string.sec_available), second);
        } else {
            if (second % TIME_BASE == 0) {
                timeString = String.format(getString(R.string.min_available), second / TIME_BASE);
            } else {
                timeString = String.format(getString(R.string.time_available), second / TIME_BASE,
                        second % TIME_BASE);
            }
        }
        LogUtil.i(TAG, "<updateRemainingTimerView> mRemainingTimeTextView.setText: "
                + timeString);
        mRemainingTimeTextView.setText(timeString);
        mRemainingTimeTextView.setVisibility(View.VISIBLE);
    }

    /**
     * release wake lock.
     */
    private void releaseWakeLock() {
        // if mWakeLock is not release, release it
        if ((null != mWakeLock) && mWakeLock.isHeld()) {
            mWakeLock.release();
            LogUtil.i(TAG, "<releaseWakeLock>");
        }
    }

    /**
     * acquire wake lock.
     */
    private void acquireWakeLock() {
        if ((null != mWakeLock) && !mWakeLock.isHeld()) {
            mWakeLock.acquire();
            LogUtil.i(TAG, "<acquireWakeLock>");
        }
    }

    private void updateOptionsMenu(boolean isShow) {
        LogUtil.i(TAG, "<updateOptionsMenu>");
        if (null == mMenu) {
            LogUtil.i(TAG, "<updateOptionsMenu> mMenu == null, return");
            return;
        }

        boolean allowSelectFormatAndMode = mRunFromLauncher && isShow;
        if (null != mService) {
            allowSelectFormatAndMode = mRunFromLauncher && isShow
                    && (SoundRecorderService.STATE_IDLE == mService.getCurrentState());
        }

        if (RecordParamsSetting.canSelectFormat()) {
            MenuItem item1 = mMenu.getItem(OPTIONMENU_SELECT_FORMAT);
            if (null != item1) {
                item1.setVisible(allowSelectFormatAndMode);
            }
        }
    }

    private void showStorageHint(String message) {
        if (null == mStorageHint) {
            mStorageHint = OnScreenHint.makeText(this, message);
        } else {
            mStorageHint.setText(message);
        }
        mStorageHint.show();
    }

    private void hideStorageHint() {
        if (null != mStorageHint) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    /**
     * for reduce repeat code.
     *
     * @param initial true to set the time as 0, otherwise set as current progress
     */
    public void setTimerTextView(boolean initial) {
        int time = 0;
        if (!initial) {
            if (null != mService) {
                time = (int) mService.getCurrentProgressInSecond();
            }
        }
        setTimerTextView(time);
    }

    private void setTimerTextView(int time) {
        LogUtil.i(TAG, "<setTimerTextView> start with time = " + time);
        String timerString = String.format(mTimerFormat, time / TIME_BASE, time % TIME_BASE);
        setTimerViewTextSize(time);
        mTimerTextView.setText(timerString);
        LogUtil.i(TAG, "<setTimerTextView> end");
    }

    private void updateUiAccordingState(int code) {
        LogUtil.d(TAG, "updateUiAccordingState start : " + code);
        switch (code) {
            case SoundRecorderService.STATE_IDLE:
                updateUiOnIdleState();
                break;
            case SoundRecorderService.STATE_PAUSE_PLAYING:
                updateUiOnPausePlayingState();
                break;
            case SoundRecorderService.STATE_RECORDING:
                updateUiOnRecordingState();
                break;
            case SoundRecorderService.STATE_PAUSE_RECORDING:
                updateUiOnPauseRecordingState();
                break;
            case SoundRecorderService.STATE_PLAYING:
                updateUiOnPlayingState();
                break;
            default:
                break;
        }
        LogUtil.d(TAG, "updateUiAccordingState end : " + code);
    }

    /**
     * disable all buttons.
     */
    private void disableButton() {
        LogUtil.i(TAG, "<disableButton>");
        closeOptionsMenu();
        updateOptionsMenu(false);
        if (mRecordButton == null) {
            return;
        }
        mRecordButton.setEnabled(false);
        mPauseRecordingButton.setEnabled(false);
        mStopButton.setEnabled(false);
        mPlayButton.setEnabled(false);
        mFileListButton.setEnabled(false);
        mDiscardButton.setEnabled(false);
        mAcceptButton.setEnabled(false);
        mIsButtonDisabled = true;
    }

    private static class SoundRecorderHandler extends Handler {
        private WeakReference<SoundRecorderActivity> activityWeakReference;

        public SoundRecorderHandler(SoundRecorderActivity activity) {
            activityWeakReference = new WeakReference<SoundRecorderActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SoundRecorderActivity activity = activityWeakReference.get();
            if (activity != null) {
                LogUtil.i(TAG, "<handleMessage> start with msg.what-" + msg.what);
                if (null == activity.mService || activity.isFinishing()) {
                    return;
                }
                if (activity.mSavingRecordFileFromMms && msg.what != SoundRecorderService.STATE_SAVE_SUCESS) {
                    return;
                } else if (activity.mSavingRecordFileFromMms && msg.what ==
                        SoundRecorderService.STATE_SAVE_SUCESS) {
                    activity.mSavingRecordFileFromMms = false;
                }
                activity.updateOptionsMenu(true);
                String filePath = activity.mService.getCurrentFilePath();
                LogUtil.i(TAG, "<handleMessage> mService.getCurrentFilePath() = " + filePath);
                activity.mFileName = NULL_STRING;
                if (null != filePath) {
                    activity.mFileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1,
                            filePath.length());
                    activity.mFileName = (activity.mFileName.endsWith(Recorder.SAMPLE_SUFFIX)) ?
                            activity.mFileName.substring(0,
                                    activity.mFileName.lastIndexOf(Recorder.SAMPLE_SUFFIX))
                            : activity.mFileName;
                }
                LogUtil.i(TAG, "<updateUi> mRecordingFileNameTextView.setText : " + activity.mFileName);
                activity.mRecordingFileNameTextView.setText(activity.mFileName);
                activity.mAcceptButton.setText(R.string.accept);
                if (activity.mRunFromLauncher) {
                    activity.mAcceptButton.setText(R.string.save_record);
                }
                activity.hideStorageHint();
                switch (msg.what) {
                    case SoundRecorderService.STATE_IDLE:
                        activity.updateUiOnIdleState();
                        break;
                    case SoundRecorderService.STATE_PAUSE_PLAYING:
                        activity.updateUiOnPausePlayingState();
                        break;
                    case SoundRecorderService.STATE_RECORDING:
                        activity.mIsRecordStarting = false;
                        activity.updateUiOnRecordingState();
                        break;
                    case SoundRecorderService.STATE_PAUSE_RECORDING:
                        activity.updateUiOnPauseRecordingState();
                        break;
                    case SoundRecorderService.STATE_PLAYING:
                        activity.updateUiOnPlayingState();
                        break;
                    case SoundRecorderService.STATE_ERROR:
                        Bundle bundle = msg.getData();
                        int errorCode = bundle.getInt(ERROR_CODE);
                        ErrorHandle.showErrorInfo(activity, errorCode);
                        if (activity.mService != null && activity.mIsButtonDisabled) {
                            activity.updateUiAccordingState(activity.mService.getCurrentState());
                        }
                        break;
                    case SoundRecorderService.STATE_SAVE_SUCESS:
                        activity.updateUiOnSaveSuccessState();
                        SoundRecorderUtils.getToast(activity,
                                R.string.tell_save_record_success);
                        break;
                    default:
                        break;
                }
                activity.mVUMeter.invalidate();
                LogUtil.i(TAG, "<handleMessage> end");
            }
        }
    }
}
