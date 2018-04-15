package cn.way.soundrecorder.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Button;

import cn.way.soundrecorder.R;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class DeleteDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "SR/DeleteDialogFragment";
    private static final String KEY_SINGLE = "single";
    private DialogInterface.OnClickListener mClickListener = null;

    /**
     * create a instance of DeleteDialogFragment.
     *
     * @param single if the number of files to be deleted is only one ?
     * @return the instance of DeleteDialogFragment
     */
    public static DeleteDialogFragment newInstance(Boolean single) {
        DeleteDialogFragment frag = new DeleteDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_SINGLE, single);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.i(TAG, "<onCreateDialog>");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String alertMsg = null;
        if (getArguments().getBoolean(KEY_SINGLE)) {
            alertMsg = getString(R.string.alert_delete_single);
        } else {
            alertMsg = getString(R.string.alert_delete_multiple);
        }

        builder.setTitle(R.string.delete).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                alertMsg).setPositiveButton(getString(R.string.ok), this).setNegativeButton(
                getString(R.string.cancel), null);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface arg0, int arg1) {
        if (null != mClickListener) {
            mClickListener.onClick(arg0, arg1);
        }
    }

    /**
     * set listener of OK button.
     *
     * @param listener the listener to be set
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mClickListener = listener;
    }

    /**
     * update the message of dialog, single/multi file/files to be deleted.
     *
     * @param single if single file to be deleted
     */
    public void setSingle(boolean single) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (null != dialog) {
            if (single) {
                dialog.setMessage(getString(R.string.alert_delete_single));
            } else {
                dialog.setMessage(getString(R.string.alert_delete_multiple));
            }
        }
    }

    /**
     * change the buttons to disable or enable.
     *
     * @param whichButton to be setting state
     * @param isEnable    whether enable button or disable
     */
    public void setButton(int whichButton, boolean isEnable) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (null != dialog) {
            Button btn = dialog.getButton(whichButton);
            if (btn != null) {
                btn.setEnabled(isEnable);
                LogUtil.d(TAG, " set button state to " + btn.isEnabled());
            } else {
                LogUtil.d(TAG, "get button" + whichButton + " from dialog is null ");
            }
        }
    }
}
