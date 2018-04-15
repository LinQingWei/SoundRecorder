package cn.way.soundrecorder.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import cn.way.soundrecorder.R;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : use DialogFragment to show Dialog.
 * </pre>
 */

public class ErrorDialogFragment extends DialogFragment {
    private static final String TAG = "SR/ErrorDialogFragment";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";

    /**
     * create a instance of ErrorDialogFragment.
     *
     * @param titleID
     * @param messageID
     * @return
     */
    public static ErrorDialogFragment newInstance(int titleID, int messageID) {
        LogUtil.i(TAG, "<newInstance> begin");
        ErrorDialogFragment frag = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, titleID);
        args.putInt(KEY_MESSAGE, messageID);
        frag.setArguments(args);
        LogUtil.i(TAG, "<newInstance> end");
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.i(TAG, "<onCreateDialog> begin");
        Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.button_ok, null).setCancelable(false);
        if (args.getInt(KEY_TITLE) > 0) {
            builder.setTitle(getString(args.getInt(KEY_TITLE)));
        }
        if (args.getInt(KEY_MESSAGE) > 0) {
            builder.setMessage(getString(args.getInt(KEY_MESSAGE)));
        }
        LogUtil.i(TAG, "<onCreateDialog> end");
        Dialog dialog = builder.create();
        LogUtil.i(TAG, "<onCreateDialog> dialog is " + dialog.toString());
        return dialog;
    }
}
