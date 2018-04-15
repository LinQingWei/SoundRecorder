package cn.way.soundrecorder;

import java.util.List;

/**
 * <pre>
 *     author: Way Lin
 *     date  : 2018.04.15
 *     desc  : save current status of list view in RecordingFileList.
 * </pre>
 */

public class ListViewProperty {
    private List<Integer> mCheckedList;
    private int mCurPos;
    private int mTop;

    /**
     * construction of ListViewProperty.
     *
     * @param list   the index list of current checked item
     * @param curPos the index of current current position
     * @param top    the index of the top item
     */
    public ListViewProperty(List<Integer> list, int curPos, int top) {
        mCheckedList = list;
        mCurPos = curPos;
        mTop = top;
    }

    /**
     * return the current check status of list view.
     *
     * @return the mCheckedList
     */
    public List<Integer> getCheckedList() {
        return mCheckedList;
    }

    /**
     * save checked list.
     *
     * @param checkedList the mCheckedList to set
     */
    public void setCheckedList(List<Integer> checkedList) {
        this.mCheckedList = checkedList;
    }

    /**
     * return the current position of list view.
     *
     * @return the mCurPos
     */
    public int getCurPos() {
        return mCurPos;
    }

    /**
     * set the current position of list view.
     *
     * @param curPos the mCurPos to set
     */
    public void setCurPos(int curPos) {
        this.mCurPos = curPos;
    }

    /**
     * get the top position of list view.
     *
     * @return the mTop
     */
    public int getTop() {
        return mTop;
    }

    /**
     * set the top position of list view.
     *
     * @param top the mTop to set
     */
    public void setTop(int top) {
        this.mTop = top;
    }
}
