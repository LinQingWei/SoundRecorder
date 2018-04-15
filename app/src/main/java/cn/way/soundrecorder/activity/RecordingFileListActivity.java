package cn.way.soundrecorder.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.way.soundrecorder.ErrorHandle;
import cn.way.soundrecorder.ListViewProperty;
import cn.way.soundrecorder.R;
import cn.way.soundrecorder.adapter.EditViewAdapter;
import cn.way.soundrecorder.fragment.DeleteDialogFragment;
import cn.way.soundrecorder.fragment.ProgressDialogFragment;
import cn.way.soundrecorder.service.SoundRecorderService;
import cn.way.soundrecorder.util.LogUtil;
import cn.way.soundrecorder.util.PermissionUtil;
import cn.way.soundrecorder.util.SoundRecorderUtils;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class RecordingFileListActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SR/RecordingFileList";
    private static final String DIALOG_TAG_DELETE = "Delete";
    private static final String DIALOG_TAG_PROGRESS = "Progress";
    private static final String RECORDING_FILELIST_DATA = "recording_filelist_data";
    private static final String REMOVE_PROGRESS_DIALOG_KEY = "remove_progress_dialog";
    public static final int NORMAL = 1;
    public static final int EDIT = 2;

    private static final String PATH = "path";
    private static final String DURATION = "duration";
    private static final String FILE_NAME = "filename";
    private static final String CREAT_DATE = "creatdate";
    private static final String FORMAT_DURATION = "formatduration";
    private static final String RECORD_ID = "recordid";
    private static final int PATH_INDEX = 2;
    private static final int DURATION_INDEX = 3;
    private static final int CREAT_DATE_INDEX = 5;
    private static final int RECORD_ID_INDEX = 7;
    private static final int ONE_SECOND = 1000;
    private static final int TIME_BASE = 60;
    private static final int NO_CHECK_POSITION = -1;
    private static final int DEFAULT_SLECTION = -1;
    private int mCurrentAdapterMode = NORMAL;
    private int mSelection = 0;
    private int mTop = 0;
    private boolean mNeedRemoveProgressDialog = false;

    private final ArrayList<HashMap<String, Object>> mArrlist =
            new ArrayList<HashMap<String, Object>>();
    private final ArrayList<String> mNameList = new ArrayList<String>();
    private final ArrayList<String> mPathList = new ArrayList<String>();
    private final ArrayList<String> mTitleList = new ArrayList<String>();
    private final ArrayList<String> mDurationList = new ArrayList<String>();
    private final List<Integer> mIdList = new ArrayList<Integer>();
    private List<Integer> mCheckedList = new ArrayList<Integer>();

    private BroadcastReceiver mSDCardMountEventReceiver = null;
    private boolean mActivityForeground = true;

    private ListView mRecordingFileListView;
    private ImageButton mRecordButton;
    private ImageButton mDeleteButton;
    private View mEmptyView;
    private QueryDataTask mQueryTask;
    private Handler mHandler;

    private final DialogInterface.OnClickListener mDeleteDialogListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    LogUtil.i(TAG, "<mDeleteDialogListener onClick>");
                    deleteItems();
                    arg0.dismiss();
                }
            };

    @Override
    public void onCreate(Bundle icycle) {
        super.onCreate(icycle);
        LogUtil.i(TAG, "<onCreate> begin");
        if (!PermissionUtil.isGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            finish();
        }
        setContentView(R.layout.activity_recording_file_list);
        mRecordingFileListView = (ListView) findViewById(R.id.recording_file_list_view);
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        mDeleteButton = (ImageButton) findViewById(R.id.deleteButton);
        mEmptyView = findViewById(R.id.empty_view);
        mRecordButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mRecordingFileListView.setOnCreateContextMenuListener(this);
        mHandler = new Handler();
        mRecordingFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long itemId) {
                if (EDIT == mCurrentAdapterMode) {
                    int id = (int) ((EditViewAdapter) mRecordingFileListView.getAdapter())
                            .getItemId(position);
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.record_file_checkbox);
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        ((EditViewAdapter) mRecordingFileListView.getAdapter()).setCheckBox(id,
                                false);
                        int count = ((EditViewAdapter) mRecordingFileListView.getAdapter())
                                .getCheckedItemsCount();
                        if (0 == count) {
                            saveLastSelection();
                            mCurrentAdapterMode = NORMAL;
                            switchAdapterView(NO_CHECK_POSITION);
                        }
                    } else {
                        checkBox.setChecked(true);
                        ((EditViewAdapter) mRecordingFileListView.getAdapter()).setCheckBox(id,
                                true);
                    }
                } else {
                    Intent intent = new Intent();
                    HashMap<String, Object> map = (HashMap<String, Object>) mRecordingFileListView
                            .getItemAtPosition(position);
                    intent.putExtra(SoundRecorderActivity.DOWHAT, SoundRecorderActivity.PLAY);
                    if (null != map) {
                        if (null != map.get(PATH)) {
                            intent.putExtra(PATH, map.get(PATH).toString());
                        }
                        if (null != map.get(DURATION)) {
                            intent.putExtra(DURATION, Integer
                                    .parseInt(map.get(DURATION).toString()));
                        }
                    }
                    intent.setClass(RecordingFileListActivity.this,
                            SoundRecorderActivity.class);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        mRecordingFileListView
                .setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                                   long itemId) {
                        int id = 0;
                        if (EDIT == mCurrentAdapterMode) {
                            id = (int) ((EditViewAdapter) mRecordingFileListView.getAdapter())
                                    .getItemId(position);
                        } else {
                            HashMap<String, Object> map =
                                    (HashMap<String, Object>) mRecordingFileListView
                                            .getItemAtPosition(position);
                            id = (Integer) map.get(RECORD_ID);
                        }
                        if (mCurrentAdapterMode == NORMAL) {
                            saveLastSelection();
                            mCurrentAdapterMode = EDIT;
                            switchAdapterView(id);
                        }
                        return true;
                    }
                });

        registerExternalStorageListener();
        LogUtil.i(TAG, "<onCreate> end");
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        LogUtil.i(TAG, "<onRetainNonConfigurationInstance> begin");
        List<Integer> checkedList = null;
        saveLastSelection();
        if (EDIT == mCurrentAdapterMode) {
            if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
                checkedList = ((EditViewAdapter) mRecordingFileListView.getAdapter())
                        .getCheckedPosList();
                LogUtil.i(TAG, "<onRetainNonConfigurationInstance> checkedList.size() = "
                        + checkedList.size());
            }
        }
        ListViewProperty listViewProperty = new ListViewProperty(checkedList, mSelection, mTop);

        SharedPreferences prefs = getSharedPreferences(RECORDING_FILELIST_DATA, 0);
        SharedPreferences.Editor ed = prefs.edit();
        LogUtil.i(TAG, "<onRetainNonConfigurationInstance> mNeedRemoveProgressDialog = "
                + mNeedRemoveProgressDialog);
        ed.putBoolean(REMOVE_PROGRESS_DIALOG_KEY, mNeedRemoveProgressDialog);
        ed.commit();
        LogUtil.i(TAG, "<onRetainNonConfigurationInstance> end");
        return listViewProperty;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        LogUtil.i(TAG, "<onRestoreInstanceState> begin");
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG_DELETE);
        LogUtil.i(TAG, "<onRestoreInstanceState> getFragmentManager() = " + getFragmentManager());
        if (null != fragment) {
            ((DeleteDialogFragment) fragment).setOnClickListener(mDeleteDialogListener);
            LogUtil.i(TAG, "<onRestoreInstanceState> getFragmentManager() = "
                    + getFragmentManager());
        }
        SharedPreferences prefs = getSharedPreferences(RECORDING_FILELIST_DATA, 0);
        if (prefs.getBoolean(REMOVE_PROGRESS_DIALOG_KEY, false)) {
            removeOldFragmentByTag(DIALOG_TAG_PROGRESS);
        }
        mNeedRemoveProgressDialog = false;
        LogUtil.i(TAG, "<onRestoreInstanceState> end");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.i(TAG, "<onResume> begin");
        setListData(mCheckedList);
        mActivityForeground = true;
        LogUtil.i(TAG, "<onResume> end");
    }

    /**
     * This method save the selection of list view on present screen.
     */
    protected void saveLastSelection() {
        LogUtil.i(TAG, "<saveLastSelection>");
        if (null != mRecordingFileListView) {
            mSelection = mRecordingFileListView.getFirstVisiblePosition();
            View cv = mRecordingFileListView.getChildAt(0);
            if (null != cv) {
                mTop = cv.getTop();
            }
        }
    }

    /**
     * This method restore the selection saved before.
     */
    protected void restoreLastSelection() {
        LogUtil.i(TAG, "<restoreLastSelection>");
        if (mSelection >= 0) {
            mRecordingFileListView.setSelectionFromTop(mSelection, mTop);
            mSelection = DEFAULT_SLECTION;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.i(TAG, "<onStart> begin");
        enableDialogButtons(false);
        ListViewProperty listViewProperty = (ListViewProperty) getLastNonConfigurationInstance();
        if (null != listViewProperty) {
            if (null != listViewProperty.getCheckedList()) {
                mCheckedList = listViewProperty.getCheckedList();
            }
            mSelection = listViewProperty.getCurPos();
            mTop = listViewProperty.getTop();
        }
        LogUtil.i(TAG, "<onStart> end");
    }

    /**
     * bind data to list view.
     *
     * @param list the index list of current checked items
     */
    private void setListData(List<Integer> list) {
        LogUtil.i(TAG, "<setListData>");
        mRecordingFileListView.setAdapter(null);
        if (mQueryTask != null) {
            mQueryTask.cancel(false);
        }
        mQueryTask = new QueryDataTask(list);
        mQueryTask.execute();
    }

    /**
     * query sound recorder recording file data.
     *
     * @return the query list of the map from String to Object
     */
    public ArrayList<HashMap<String, Object>> queryData() {
        LogUtil.i(TAG, "<queryData>");
        mArrlist.clear();
        mNameList.clear();
        mPathList.clear();
        mTitleList.clear();
        mDurationList.clear();
        mIdList.clear();

        String where = SoundRecorderService.ALBUM_ARTIST + "=?";
        Cursor recordingFileCursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media._ID
                }, where, new String[]{"recorder"}, null);
        try {
            if ((null == recordingFileCursor) || (0 == recordingFileCursor.getCount())) {
                LogUtil.i(TAG, "<queryData> the data return by query is null");
                return null;
            }
            LogUtil.i(TAG, "<queryData> the data return by query is available");
            recordingFileCursor.moveToFirst();
            int num = recordingFileCursor.getCount();
            final int sizeOfHashMap = 6;
            String path = null;
            String fileName = null;
            int duration = 0;
            long cDate = 0;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getResources().getString(
                    R.string.audio_db_title_format));
            String createDate = null;
            int recordId = 0;
            Date date = new Date();
            for (int j = 0; j < num; j++) {
                HashMap<String, Object> map = new HashMap<String, Object>(sizeOfHashMap);
                path = recordingFileCursor.getString(PATH_INDEX);
                if (null != path) {
                    fileName = path.substring(path.lastIndexOf("/") + 1, path.length());
                }
                duration = recordingFileCursor.getInt(DURATION_INDEX);
                if (duration < ONE_SECOND) {
                    duration = ONE_SECOND;
                }
                cDate = recordingFileCursor.getInt(CREAT_DATE_INDEX);
                date.setTime(cDate * ONE_SECOND);
                createDate = simpleDateFormat.format(date);
                recordId = recordingFileCursor.getInt(RECORD_ID_INDEX);

                map.put(FILE_NAME, fileName);
                map.put(PATH, path);
                map.put(DURATION, duration);
                map.put(CREAT_DATE, createDate);
                map.put(FORMAT_DURATION, formatDuration(duration));
                map.put(RECORD_ID, recordId);

                mNameList.add(fileName);
                mPathList.add(path);
                mTitleList.add(createDate);
                mDurationList.add(formatDuration(duration));
                mIdList.add(recordId);

                recordingFileCursor.moveToNext();
                mArrlist.add(map);
            }
        } catch (IllegalStateException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ErrorHandle
                            .showErrorInfo(RecordingFileListActivity.this,
                                    ErrorHandle.ERROR_ACCESSING_DB_FAILED_WHEN_QUERY);
                }
            });
            e.printStackTrace();
        } finally {
            if (null != recordingFileCursor) {
                recordingFileCursor.close();
            }
        }
        return mArrlist;
    }

    /**
     * update UI after query data.
     *
     * @param list the list of query result
     */
    public void afterQuery(List<Integer> list) {
        LogUtil.i(TAG, "<afterQuery>");
        if (null == list) {
            mCurrentAdapterMode = NORMAL;
            switchAdapterView(NO_CHECK_POSITION);
        } else {
            list.retainAll(mIdList);
            if (list.isEmpty()) {
                removeOldFragmentByTag(DIALOG_TAG_DELETE);
                mCurrentAdapterMode = NORMAL;
                switchAdapterView(NO_CHECK_POSITION);
            } else {
                // for refresh status of DeleteDialogFragment(single/multi)
                LogUtil.i(TAG, "<afterQuery> list.size() = " + list.size());
                enableDialogButtons(true);
                setDeleteDialogSingle(1 == list.size());
                mCurrentAdapterMode = EDIT;
                EditViewAdapter adapter = new EditViewAdapter(getApplicationContext(), mNameList,
                        mPathList, mTitleList, mDurationList, mIdList, list);
                mRecordingFileListView.setAdapter(adapter);
                mDeleteButton.setVisibility(View.VISIBLE);
                mRecordButton.setVisibility(View.GONE);
                restoreLastSelection();
            }
        }
    }

    /**
     * format duration to display as 00:00.
     *
     * @param duration the duration to be format
     * @return the String after format
     */
    private String formatDuration(int duration) {
        String timerFormat = getResources().getString(R.string.timer_format);
        int time = duration / ONE_SECOND;
        return String.format(timerFormat, time / TIME_BASE, time % TIME_BASE);
    }

    @Override
    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.recordButton:
                LogUtil.i(TAG, "<onClick> recordButton");
                mRecordButton.setEnabled(false);
                Intent intent = new Intent();
                intent.setClass(this, SoundRecorderActivity.class);
                intent.putExtra(SoundRecorderActivity.DOWHAT, SoundRecorderActivity.RECORD);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.deleteButton:
                LogUtil.i(TAG, "<onClick> deleteButton");
                EditViewAdapter adapter = (EditViewAdapter) mRecordingFileListView.getAdapter();
                if (adapter != null) {
                    showDeleteDialog(adapter.getCheckedItemsCount() == 1);
                }
                break;
            default:
                break;
        }
    }

    /**
     * show DeleteDialogFragment.
     *
     * @param single if the number of files to be deleted == 0 ?
     */
    private void showDeleteDialog(boolean single) {
        LogUtil.i(TAG, "<showDeleteDialog> single = " + single);
        removeOldFragmentByTag(DIALOG_TAG_DELETE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        LogUtil.i(TAG, "<showDeleteDialog> fragmentManager = " + fragmentManager);
        DialogFragment newFragment = DeleteDialogFragment.newInstance(single);
        ((DeleteDialogFragment) newFragment).setOnClickListener(mDeleteDialogListener);
        newFragment.show(fragmentManager, DIALOG_TAG_DELETE);
        fragmentManager.executePendingTransactions();
    }

    /**
     * remove old DialogFragment.
     *
     * @param tag the tag of DialogFragment to be removed
     */
    private void removeOldFragmentByTag(String tag) {
        LogUtil.i(TAG, "<removeOldFragmentByTag> tag = " + tag);
        FragmentManager fragmentManager = getSupportFragmentManager();
        LogUtil.i(TAG, "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment oldFragment = (DialogFragment) fragmentManager.findFragmentByTag(tag);
        LogUtil.i(TAG, "<removeOldFragmentByTag> oldFragment = " + oldFragment);
        if (null != oldFragment) {
            oldFragment.dismissAllowingStateLoss();
        }
    }

    /**
     * update the message of delete dialog.
     *
     * @param single if single file to be deleted
     */
    private void setDeleteDialogSingle(boolean single) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LogUtil.i(TAG, "<setDeleteDialogSingle> fragmentManager = " + fragmentManager);
        DeleteDialogFragment oldFragment = (DeleteDialogFragment) fragmentManager
                .findFragmentByTag(DIALOG_TAG_DELETE);
        if (null == oldFragment) {
            LogUtil.i(TAG, "<setDeleteDialogSingle> no old delete dialog");
        } else {
            oldFragment.setSingle(single);
            LogUtil.i(TAG, "<setDeleteDialogSingle> setSingle single = " + single);
        }
    }

    /**
     * call delete file task.
     */
    public void deleteItems() {
        LogUtil.i(TAG, "<deleteItems> call FileTask to delete");
        FileTask fileTask = new FileTask();
        fileTask.execute();
    }

    /**
     * switch adapter mode between NORMAL and EDIT.
     *
     * @param pos the index of current clicked item
     */
    public void switchAdapterView(int pos) {
        if (NORMAL == mCurrentAdapterMode) {
            LogUtil.i(TAG, "<switchAdapterView> from edit mode to normal mode");
            SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), mArrlist,
                    R.layout.list_item_navigation, new String[]{FILE_NAME, CREAT_DATE,
                    FORMAT_DURATION}, new int[]{R.id.record_file_name,
                    R.id.record_file_title, R.id.record_file_duration});
            mRecordingFileListView.setAdapter(adapter);
            mDeleteButton.setVisibility(View.GONE);
            mRecordButton.setVisibility(View.VISIBLE);
        } else {
            LogUtil.i(TAG, "<switchAdapterView> from normal mode to edit mode");
            EditViewAdapter adapter = new EditViewAdapter(getApplicationContext(), mNameList,
                    mPathList, mTitleList, mDurationList, mIdList, pos);
            mRecordingFileListView.setAdapter(adapter);
            mDeleteButton.setVisibility(View.VISIBLE);
            mRecordButton.setVisibility(View.GONE);
        }
        restoreLastSelection();
    }

    /**
     * The method gets the selected items and create a list of File objects.
     *
     * @return a list of File objects
     */
    protected List<File> getSelectedFiles() {
        LogUtil.i(TAG, "<getSelectedFiles> begin");
        List<File> list = new ArrayList<File>();
        if (EDIT != mCurrentAdapterMode) {
            LogUtil.i(TAG, "<getSelectedFiles> end");
            return list;
        }
        if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
            List<String> checkedList = ((EditViewAdapter) mRecordingFileListView
                    .getAdapter()).getCheckedItemsList();
            int listSize = checkedList.size();
            for (int i = 0; i < listSize; i++) {
                File file = new File(checkedList.get(i));
                list.add(file);
            }
        }
        LogUtil.i(TAG, "<getSelectedFiles> end");
        return list;
    }

    @Override
    protected void onPause() {
        LogUtil.i(TAG, "<onPause> begin");
        List<Integer> checkedList = null;
        if (mQueryTask != null) {
            // should cancel async task to avoid the last task post
            // finished message to main handler when the new async task is executing.
            mQueryTask.cancel(false);
            mQueryTask = null;
        }
        if (EDIT == mCurrentAdapterMode) {
            if (null != ((EditViewAdapter) mRecordingFileListView.getAdapter())) {
                checkedList = ((EditViewAdapter) mRecordingFileListView.getAdapter())
                        .getCheckedPosList();
                if (!checkedList.isEmpty()) {
                    mCheckedList = checkedList;
                    LogUtil.i(TAG,
                            "<onPause> mCheckedList.size() = " + mCheckedList.size());
                }
            }
        } else {
            mCheckedList = null;
        }
        mActivityForeground = false;
        saveLastSelection();
        LogUtil.i(TAG, "<onPause> end");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        LogUtil.i(TAG, "onBackPressed");
        if (EDIT == mCurrentAdapterMode) {
            mCurrentAdapterMode = NORMAL;
            saveLastSelection();
            switchAdapterView(NO_CHECK_POSITION);
        } else {
            finishSelf();
            super.onBackPressed();
        }
    }

    /**
     * FileTask for delete some recording file.
     */
    public class FileTask extends AsyncTask<Void, Object, Boolean> {
        /**
         * A callback method to be invoked before the background thread starts
         * running.
         */
        @Override
        protected void onPreExecute() {
            LogUtil.i(TAG, "<FileTask.onPreExecute>");
            if (mActivityForeground) {
                LogUtil.i(TAG, "<FileTask.onPreExecute> Activity is running in foreground");
                FragmentManager fragmentManager = getSupportFragmentManager();
                LogUtil.i(TAG, "<FileTask.onPreExecute> fragmentManager = " + fragmentManager);
                DialogFragment newFragment = ProgressDialogFragment.newInstance();
                newFragment.show(fragmentManager, DIALOG_TAG_PROGRESS);
                fragmentManager.executePendingTransactions();
            } else {
                LogUtil.i(TAG, "<FileTask.onPreExecute> Activity is running in background");
            }
        }

        /**
         * A callback method to be invoked when the background thread starts
         * running.
         *
         * @param params the method need not parameters here
         * @return true/false, success or fail
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            LogUtil.i(TAG, "<FileTask.doInBackground> begin");
            // delete files
            List<File> list = getSelectedFiles();
            int listSize = list.size();
            LogUtil.i(TAG, "<FileTask.doInBackground> the number of delete files: " + listSize);
            for (int i = 0; i < listSize; i++) {
                File file = list.get(i);
                if (null != file) {
                    LogUtil.i(TAG,
                            "<doInBackground>, the file to be delete is:" + file.getAbsolutePath());
                }
                if (!file.delete()) {
                    LogUtil.i(TAG, "<FileTask.doInBackground> delete file ["
                            + list.get(i).getAbsolutePath() + "] fail");
                }
                if (!SoundRecorderUtils.deleteFileFromMediaDB(getApplicationContext(),
                        file.getAbsolutePath())) {
                    return false;
                }
            }
            LogUtil.i(TAG, "<FileTask.doInBackground> end");
            return true;
        }

        /**
         * A callback method to be invoked after the background thread performs
         * the task.
         *
         * @param result the value returned by doInBackground()
         */
        @Override
        protected void onPostExecute(Boolean result) {
            LogUtil.i(TAG, "<FileTask.onPostExecute>");
            removeOldFragmentByTag(DIALOG_TAG_PROGRESS);
            mNeedRemoveProgressDialog = true;
            if (mActivityForeground) {
                mCurrentAdapterMode = NORMAL;
                if (!result) {
                    ErrorHandle.showErrorInfo(RecordingFileListActivity.this,
                            ErrorHandle.ERROR_DELETING_FAILED);
                }
                setListData(null);
            }
        }

        /**
         * A callback method to be invoked when the background thread's task is
         * cancelled.
         */
        @Override
        protected void onCancelled() {
            LogUtil.i(TAG, "<FileTask.onCancelled>");
            FragmentManager fragmentManager = getSupportFragmentManager();
            LogUtil.i(TAG, "<FileTask.onCancelled> fragmentManager = " + fragmentManager);
            DialogFragment oldFragment = (DialogFragment) fragmentManager
                    .findFragmentByTag(DIALOG_TAG_PROGRESS);
            if (null != oldFragment) {
                oldFragment.dismissAllowingStateLoss();
            }
        }
    }

    /**
     * through AsyncTask to query recording file data from database.
     */
    public class QueryDataTask extends AsyncTask<Void, Object, ArrayList<HashMap<String, Object>>> {
        List<Integer> mList;

        /**
         * the construction of QueryDataTask.
         *
         * @param list the index list of current checked items
         */
        QueryDataTask(List<Integer> list) {
            mList = list;
        }

        /**
         * query data from database.
         *
         * @param params no parameter
         * @return the query result
         */
        protected ArrayList<HashMap<String, Object>> doInBackground(Void... params) {
            LogUtil.i(TAG, "<QueryDataTask.doInBackground>");
            return queryData();
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, Object>> result) {
            LogUtil.i(TAG, "<QueryDataTask.onPostExecute>");
            if (mQueryTask == QueryDataTask.this) {
                mQueryTask = null;
            }
            if (QueryDataTask.this.isCancelled()) {
                LogUtil.i(TAG, "<QueryDataTask.onPostExceute> task is cancelled, return.");
                return;
            }
            if (mActivityForeground) {
                if (null == result) {
                    removeOldFragmentByTag(DIALOG_TAG_DELETE);
                    mRecordingFileListView.setEmptyView(mEmptyView);
                    mDeleteButton.setVisibility(View.GONE);
                    mRecordButton.setVisibility(View.VISIBLE);
                } else {
                    afterQuery(mList);
                }
            }
        }
    }

    /**
     * setResult to SoundRecorderActivity and finish self.
     */
    public void finishSelf() {
        LogUtil.i(TAG, "<finishSelf>");
        mCurrentAdapterMode = NORMAL;
        Intent intent = new Intent();
        intent.setClass(this, SoundRecorderActivity.class);
        intent.putExtra(SoundRecorderActivity.DOWHAT, SoundRecorderActivity.INIT);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onDestroy() {
        LogUtil.i(TAG, "<onDestroy> begin");
        if (null != mSDCardMountEventReceiver) {
            unregisterReceiver(mSDCardMountEventReceiver);
            mSDCardMountEventReceiver = null;
        }
        LogUtil.i(TAG, "<onDestroy> end");
        super.onDestroy();
    }

    /**
     * deal with SDCard mount and eject event.
     */
    private void registerExternalStorageListener() {
        LogUtil.i(TAG, "<registerExternalStorageListener>");
        if (null == mSDCardMountEventReceiver) {
            mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null && (action.equals(Intent.ACTION_MEDIA_EJECT)
                            || action.equals(Intent.ACTION_MEDIA_UNMOUNTED))) {
                        mRecordingFileListView.setAdapter(null);
                        mCheckedList = null;
                        ErrorHandle.showErrorInfo(RecordingFileListActivity.this,
                                ErrorHandle.ERROR_SD_UNMOUNTED_ON_FILE_LIST);
                        finishSelf();
                    } else if (action != null && action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        setListData(mCheckedList);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mSDCardMountEventReceiver, iFilter);
        }
    }

    private void enableDialogButtons(boolean isEnabled) {
        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG_DELETE);
        if (null != fragment) {
            LogUtil.d(TAG, "enableDialogButtons " + isEnabled);
            ((DeleteDialogFragment) fragment).setButton(DialogInterface.BUTTON_POSITIVE, isEnabled);
            ((DeleteDialogFragment) fragment).setButton(DialogInterface.BUTTON_NEGATIVE, isEnabled);
        }
    }
}
