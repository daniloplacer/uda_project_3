package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the CursorRecyclerViewApater.java code and idea.
 */
public abstract class CursorRecyclerViewAdapter
        <VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>{

    private static final String LOG_TAG = CursorRecyclerViewAdapter.class.getSimpleName();
    private Cursor mCursor;

    private Context mContext;

    // Initially, indicates whether the cursor is null, and then is updated
    // by NotifyingDataSetObserver depending on the success of the data change
    private boolean dataIsValid;

    // Column index of the ID from the cursor, so can access it more easily
    private int rowIdColumn;

    // Receives call backs when a data set has been changed, or made invalid
    private DataSetObserver mDataSetObserver;

    // Sends this broadcast message once data is updated
    public static final String ACTION_DATA_UPDATE =
            "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";


    public CursorRecyclerViewAdapter(Context context, Cursor cursor){

        mContext = context;

        mCursor = cursor;
        dataIsValid = cursor != null;
        rowIdColumn = dataIsValid ? mCursor.getColumnIndex("_id") : -1;

        // Register an observer that is called when changes happen to the contents of the cursor
        // Its basically used to change the status of dataIsValid attribute
        mDataSetObserver = new NotifyingDataSetObserver();

        if (dataIsValid){
            mCursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    public Cursor getCursor(){
        return mCursor;
    }

    @Override
    public int getItemCount(){
        if (dataIsValid && mCursor != null){
            return mCursor.getCount();
        }

        return 0;
    }

    @Override public long getItemId(int position) {
        if (dataIsValid && mCursor != null && mCursor.moveToPosition(position)){
            return mCursor.getLong(rowIdColumn);
        }
        return 0;
    }

    // Optimization to avoid reordering when dataset is changed
    @Override public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        if (!dataIsValid){
            throw new IllegalStateException("This should only be called when Cursor is valid");
        }
        if (!mCursor.moveToPosition(position)){
            throw new IllegalStateException("Could not move Cursor to position: " + position);
        }

        onBindViewHolder(viewHolder, mCursor);
    }

    public Cursor swapCursor(Cursor newCursor){

        if (newCursor == mCursor){
            return null;
        }

        final Cursor oldCursor = mCursor;

        if (oldCursor != null && mDataSetObserver != null){
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;

        if (mCursor != null){
            if (mDataSetObserver != null){
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow("_id");
            dataIsValid = true;
            notifyDataSetChanged();
        } else {
            rowIdColumn = -1;
            dataIsValid = false;
            notifyDataSetChanged();
        }

        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            super.onChanged();

            dataIsValid = true;
            notifyDataSetChanged();

        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            dataIsValid = false;
            notifyDataSetChanged();
        }
    }
}
