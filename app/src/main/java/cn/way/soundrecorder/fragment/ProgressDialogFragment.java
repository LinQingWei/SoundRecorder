package cn.way.soundrecorder.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import cn.way.soundrecorder.R;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class ProgressDialogFragment extends DialogFragment {
    private static final String TAG = "SR/ProgressDialogFragment";

    /**
     * M: create a instance of ProgressDialogFragment.
     *
     * @return the instance of ProgressDialogFragment
     */
    public static ProgressDialogFragment newInstance() {
        return new ProgressDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.i(TAG, "<onCreateDialog>");
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(getString(R.string.delete));
        dialog.setMessage(getString(R.string.deleting));
        dialog.setCancelable(false);
        return dialog;
    }
}
