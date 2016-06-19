package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.QuoteDataPoint;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.SingleStockIntentService;
import com.sam_chordas.android.stockhawk.service.SingleStockTaskService;

import java.util.ArrayList;

/**
 * Created by daniloplacer on 6/4/16.
 */
public class StockDetailsActivity extends AppCompatActivity {

    private String LOG_TAG = StockDetailsActivity.class.getSimpleName();

    public final static String INTENT_EXTRA_STOCK_SYMBOL = "stock_symbol";

    private final static String STATE_SYMBOL = "state_symbol";
    private final static String STATE_DATES = "state_dates";
    private final static String STATE_VALUES = "state_values";
    private final static String STATE_LOWER_BOUND = "state_lower_bound";
    private final static String STATE_UPPER_BOUND = "state_upper_bound";
    private final static String STATE_CHART_SHOWN = "chart_shown";

    private LineChartView mLineChart;

    private boolean mChartShown;

    private String mSymbol;
    private String[] mDates;
    private float[] mValues;
    private float mLowerBound, mUpperBound;

    // Handler called when SingleStockTaskService finishes loading historic stock data
    // Updates the historic stock price chart
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            ArrayList<QuoteDataPoint> dataPoints =
                    intent.getParcelableArrayListExtra(SingleStockTaskService.INTENT_EXTRA_STOCK_HISTORY);

            int arraySize = dataPoints.size();

            if (arraySize > 0) {

                mDates = new String[arraySize];
                mValues = new float[arraySize];

                mLowerBound = dataPoints.get(arraySize-1).getBidValue();
                mUpperBound = 0;

                // iterates through the data points array to put
                // the dates and values in different, separate arrays
                for (int i = 0; i < arraySize; i++) {

                    float value = dataPoints.get(arraySize-1-i).getBidValue();

                    mDates[i] = dataPoints.get(arraySize-1-i).getDate();
                    mValues[i] = value;

                    // Determines the lower and upper bound for the chart
                    if (value > mUpperBound) {
                        mUpperBound = value;
                    } else {
                        if (value < mLowerBound) {
                            mLowerBound = value;
                        }
                    }

                }

                drawChart();

            }

        }

    };

    // Effectively draws mLineChart based on values from mDates & mValues arrays,
    // with mLowerBound and mUpperBound as chart limits
    private void drawChart() {

        mLineChart.setAxisBorderValues((int) mLowerBound-1, (int) mUpperBound+1, 1);
        mLineChart.setAxisColor(Color.WHITE);
        mLineChart.setLabelsColor(Color.WHITE);

        LineSet line = new LineSet(mDates, mValues);
        line.setColor(Color.YELLOW);

        mLineChart.addData(line);
        mLineChart.show();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putBoolean(STATE_CHART_SHOWN, mChartShown);

        if (mChartShown && (mDates != null) && (mValues != null)) {
            savedInstanceState.putString(STATE_SYMBOL, mSymbol);
            savedInstanceState.putStringArray(STATE_DATES, mDates);
            savedInstanceState.putFloatArray(STATE_VALUES, mValues);
            savedInstanceState.putFloat(STATE_LOWER_BOUND, mLowerBound);
            savedInstanceState.putFloat(STATE_UPPER_BOUND, mUpperBound);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stock_details);
        TextView textView = (TextView) findViewById(R.id.detail_stock_symbol);

        if (savedInstanceState != null) {

            mChartShown = savedInstanceState.getBoolean(STATE_CHART_SHOWN);

            if (mChartShown) {

                mLineChart = (LineChartView) findViewById(R.id.linechart);

                mSymbol = savedInstanceState.getString(STATE_SYMBOL);
                mDates = savedInstanceState.getStringArray(STATE_DATES);
                mValues = savedInstanceState.getFloatArray(STATE_VALUES);
                mLowerBound = savedInstanceState.getFloat(STATE_LOWER_BOUND, 0);
                mUpperBound = savedInstanceState.getFloat(STATE_UPPER_BOUND, 0);

                textView.setText(mSymbol);
                drawChart();

            } else {
                textView.setText(R.string.details_empty_text);
            }

        } else {

            if (Utils.isNetworkAvailable(this)) {

                mChartShown = true;
                mLineChart = (LineChartView) findViewById(R.id.linechart);

                // Gets the stock symbol from intent and updates the screen with it
                Intent intent = getIntent();
                mSymbol = intent.getStringExtra(INTENT_EXTRA_STOCK_SYMBOL);

                textView.setText(mSymbol);

                String startDate = Utils.getStartDate();
                String endDate = Utils.getEndDate();

                // Starts the intent service to pull historic stock data
                Intent serviceIntent = new Intent(this, SingleStockIntentService.class);
                serviceIntent.putExtra(SingleStockTaskService.PARAMS_EXTRA_SYMBOL, mSymbol);
                serviceIntent.putExtra(SingleStockTaskService.PARAMS_EXTRA_START_DATE, startDate);
                serviceIntent.putExtra(SingleStockTaskService.PARAMS_EXTRA_END_DATE, endDate);

                startService(serviceIntent);

            } else {

                mChartShown = false;
                textView.setText(R.string.details_empty_text);
            }

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Registers the broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(SingleStockTaskService.INTENT_STOCK_HISTORY));

    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver since the activity is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        super.onPause();
    }

}
