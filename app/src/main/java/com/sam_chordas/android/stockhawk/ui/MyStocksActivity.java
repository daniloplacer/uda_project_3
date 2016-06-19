package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.widget.CollectionWidgetProvider;

public class MyStocksActivity
        extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;

    // Our handler for received Intents. This will be called whenever an Intent
    // is broadcasted informing that stock symbol wasn't found
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String stock = intent.getStringExtra(StockTaskService.INTENT_EXTRA_STOCK_SYMBOL);

            Toast toast = Toast.makeText(
                    mContext,
                    String.format(getString(R.string.symbol_search_symbol_not_found_toast), stock),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
            toast.show();
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        mTitle = getTitle();

        isConnected = Utils.isNetworkAvailable(mContext);

        // Main layout contains RecyclerView and button to add stocks
        setContentView(R.layout.activity_my_stocks);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null) {

            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");

            if (isConnected) {
                startService(mServiceIntent);
            } else {
                // If connection is not available, needs to inform the user
                Toast.makeText(mContext, getString(R.string.update_network_toast), Toast.LENGTH_LONG).show();
            }

        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Starts the loader to query the database
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        // The cursor adapter data will be updated once the loader finishes loading
        mCursorAdapter = new QuoteCursorAdapter(this, null);

        // Sets up the click listener for each list item
        recyclerView.addOnItemTouchListener(
                new RecyclerViewItemClickListener(this,

                        new RecyclerViewItemClickListener.OnItemClickListener() {

                            @Override
                            public void onItemClick(View v, int position) {

                                isConnected = Utils.isNetworkAvailable(mContext);

                                if (isConnected) {

                                    mCursor.moveToPosition(position);

                                    String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));

                                    Intent intent = new Intent(MyStocksActivity.this, StockDetailsActivity.class);
                                    intent.putExtra(StockDetailsActivity.INTENT_EXTRA_STOCK_SYMBOL, symbol);
                                    startActivity(intent);

                                } else {

                                    // If connection is not available, needs to inform the user
                                    Toast.makeText(mContext, getString(R.string.details_network_toast), Toast.LENGTH_LONG).show();

                                }


                            }

                        }
                )
        );

        recyclerView.setAdapter(mCursorAdapter);

        // Sets up the add stock floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        // FAB will hide as the list is scrolled down and revealed as the list is scrolled up
        fab.attachToRecyclerView(recyclerView);

        fab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (isConnected){

                            // 3rd party dialog API
                            new MaterialDialog.Builder(mContext).title(R.string.symbol_search_title)
                                    .content(R.string.symbol_search_description)
                                    .inputType(InputType.TYPE_CLASS_TEXT)
                                    .input(R.string.symbol_search_hint, R.string.symbol_search_prefill, new MaterialDialog.InputCallback() {
                                        @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                                            // On FAB click, receive user input. Make sure the stock doesn't already exist
                                            // in the DB and proceed accordingly
                                            Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                    new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                                                    new String[] { input.toString() }, null);

                                            if (c.getCount() != 0) {
                                                Toast toast =
                                                        Toast.makeText(MyStocksActivity.this, R.string.symbol_search_symbol_exists_toast,
                                                                Toast.LENGTH_LONG);
                                                toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                                toast.show();
                                                return;
                                            } else {
                                                // Add the stock to DB
                                                mServiceIntent.putExtra("tag", "add");
                                                mServiceIntent.putExtra("symbol", input.toString());
                                                startService(mServiceIntent);
                                            }
                                        }
                                    })
                                    .show();
                        } else {
                            // In case not connected, need to send a message to the user
                            Toast.makeText(mContext, getString(R.string.add_network_toast), Toast.LENGTH_LONG).show();
                        }

                    }
                });

        // ItemTouchHelper is a utility class to add swipe to dismiss and drag & drop support to RecyclerView
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        setPeriodicPull();

    }

    // Creates a periodic task to pull stocks once every hour after the app has been opened
    // This is so Widget data stays up to date.
    private void setPeriodicPull() {

        if (isConnected){
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();

            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

        // Registers the broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(StockTaskService.INTENT_STOCK_NOT_FOUND));

    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver since the activity is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);

        super.onPause();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units){
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        mCursorAdapter.swapCursor(data);
        mCursor = data;

        Intent intent = new Intent(this, CollectionWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        sendBroadcast(intent);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
    }

}
