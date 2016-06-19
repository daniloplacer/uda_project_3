package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by daniloplacer on 6/18/16.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CollectionWidgetRemoveViewsService extends RemoteViewsService {

    private static final String[] QUOTE_COLUMNS = {
            QuoteDatabase.QUOTES + "." + QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE
    };

    static final int INDEX_STOCK_ID = 0;
    static final int INDEX_SYMBOL = 1;
    static final int INDEX_BIDPRICE = 2;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() { }

            // Called after onCreate as well
            @Override
            public void onDataSetChanged() {

                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                // Similar to this call at MyStocksActivity
                data = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        QUOTE_COLUMNS,
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);

                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            // Binding happens here
            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.collection_widget_list_item);

                String symbol = data.getString(INDEX_SYMBOL);
                String bidPrice = data.getString(INDEX_BIDPRICE);

                views.setTextViewText(R.id.collection_widget_stock_symbol, symbol);
                views.setTextViewText(R.id.collection_widget_bid_price, bidPrice);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(StockDetailsActivity.INTENT_EXTRA_STOCK_SYMBOL, symbol);
                views.setOnClickFillInIntent(R.id.collection_widget_list_item, fillInIntent);

                return views;

            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.collection_widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) {
                    data.getLong(INDEX_STOCK_ID);
                }

                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}
