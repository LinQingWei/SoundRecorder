package cn.way.soundrecorder.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import cn.way.soundrecorder.R;
import cn.way.soundrecorder.RecordParamsSetting;
import cn.way.soundrecorder.util.LogUtil;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  :
 * </pre>
 */

public class SelectDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
    private static final String TAG = "SR/SelectDialogFragment";
    private static final String KEY_ITEM_ARRAY = "itemArray";
    private static final String KEY_SUFFIX_ARRAY = "suffixArray";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DEFAULT_SELECT = "nowSelect";
    private static final String KEY_DEFAULT_SELECTARRAY = "nowSelectArray";
    private static final String KEY_SINGLE_CHOICE = "singleChoice";
    private DialogInterface.OnClickListener mClickListener;
    private DialogInterface.OnMultiChoiceClickListener mMultiChoiceClickListener;

    /**
     * create a instance of SelectDialogFragment.
     *
     * @param itemArrayID    the resource id array of strings that show in list
     * @param suffixArray    the suffix array at the right of list item
     * @param titleID        the resource id of title string
     * @param singleChoice   single choice or not
     * @param nowSelect      the current select item index
     * @param nowSelectArray array of now selected
     * @return the instance of SelectDialogFragment
     */
    public static SelectDialogFragment newInstance(int[] itemArrayID, CharSequence[] suffixArray,
                                                   int titleID, boolean singleChoice,
                                                   int nowSelect, boolean[] nowSelectArray) {
        SelectDialogFragment frag = new SelectDialogFragment();
        Bundle args = new Bundle();
        args.putIntArray(KEY_ITEM_ARRAY, itemArrayID);
        args.putCharSequenceArray(KEY_SUFFIX_ARRAY, suffixArray);
        args.putInt(KEY_TITLE, titleID);
        args.putBoolean(KEY_SINGLE_CHOICE, singleChoice);
        if (singleChoice) {
            args.putInt(KEY_DEFAULT_SELECT, nowSelect);
        } else {
            args.putBooleanArray(KEY_DEFAULT_SELECTARRAY, nowSelectArray.clone());
        }
        frag.setArguments(args);
        return frag;
    }

    /**
     * create a instance of SelectDialogFragment.
     *
     * @param itemArrayString array of strings that show in list
     * @param suffixArray     the suffix array at the right of list item
     * @param titleID         the resource id of title string
     * @param singleChoice    single choice or not
     * @param nowSelect       the current select item index
     * @param nowSelectArray  array of now selected
     * @return the instance of SelectDialogFragment
     */
    public static SelectDialogFragment newInstance(String[] itemArrayString, CharSequence[] suffixArray,
                                                   int titleID, boolean singleChoice,
                                                   int nowSelect, boolean[] nowSelectArray) {
        SelectDialogFragment frag = new SelectDialogFragment();
        Bundle args = new Bundle();
        args.putStringArray(KEY_ITEM_ARRAY, itemArrayString);
        args.putCharSequenceArray(KEY_SUFFIX_ARRAY, suffixArray);
        args.putInt(KEY_TITLE, titleID);
        args.putBoolean(KEY_SINGLE_CHOICE, singleChoice);
        if (singleChoice) {
            args.putInt(KEY_DEFAULT_SELECT, nowSelect);
        } else {
            args.putBooleanArray(KEY_DEFAULT_SELECTARRAY, nowSelectArray.clone());
        }
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.i(TAG, "<onCreateDialog>");
        Bundle args = getArguments();
        final String title = getString(args.getInt(KEY_TITLE));
        CharSequence[] itemArray = null;
        if (args.getInt(KEY_TITLE) == R.string.select_voice_quality) {
            itemArray = appendSurffix(RecordParamsSetting
                            .getFormatStringArray(this.getActivity()),
                    args.getCharSequenceArray(KEY_SUFFIX_ARRAY));
        } else {
            itemArray = appendSurffix(args.getIntArray(KEY_ITEM_ARRAY),
                    args.getCharSequenceArray(KEY_SUFFIX_ARRAY));
        }

        final boolean singleChoice = args.getBoolean(KEY_SINGLE_CHOICE);
        AlertDialog.Builder builder = null;
        if (singleChoice) {
            int nowSelect = args.getInt(KEY_DEFAULT_SELECT);
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).setSingleChoiceItems(itemArray, nowSelect, this)
                    .setNegativeButton(getString(R.string.cancel), null);
        } else {
            boolean[] nowSelectArray = args.getBooleanArray(KEY_DEFAULT_SELECTARRAY);
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).setMultiChoiceItems(itemArray, nowSelectArray, this)
                    .setNegativeButton(getString(R.string.cancel), null).setPositiveButton(
                    getString(R.string.ok), this);
        }
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (null != mClickListener) {
            mClickListener.onClick(dialog, which);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (null != mMultiChoiceClickListener) {
            mMultiChoiceClickListener.onClick(dialog, which, isChecked);
        }
    }

    /**
     * set listener of click items.
     *
     * @param listener the listener to be set
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mClickListener = listener;
    }

    public void setOnMultiChoiceListener(DialogInterface.OnMultiChoiceClickListener listener) {
        mMultiChoiceClickListener = listener;
    }

    private CharSequence[] appendSurffix(int[] itemStringId, CharSequence[] suffix) {
        if (null == itemStringId) {
            return null;
        }
        CharSequence[] itemArray = new CharSequence[itemStringId.length];
        for (int i = 0; i < itemStringId.length; i++) {
            itemArray[i] = getString(itemStringId[i]) + ((suffix != null) ? suffix[i] : "");
        }
        return itemArray;
    }

    private CharSequence[] appendSurffix(String[] itemString, CharSequence[] suffix) {
        if (null == itemString) {
            return null;
        }
        CharSequence[] itemArray = new CharSequence[itemString.length];
        for (int i = 0; i < itemString.length; i++) {
            itemArray[i] = itemString[i] + ((suffix != null) ? suffix[i] : "");
        }
        return itemArray;
    }
}
