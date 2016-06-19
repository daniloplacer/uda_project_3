package com.sam_chordas.android.stockhawk.service;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.rest.QuoteDataPoint;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SingleStockTaskService extends GcmTaskService{

    private String LOG_TAG = SingleStockTaskService.class.getSimpleName();

    public final static String INTENT_STOCK_HISTORY = "stock_history";
    public final static String INTENT_EXTRA_STOCK_HISTORY = "extra_stock_history";

    public final static String PARAMS_TAG = "search";
    public final static String PARAMS_EXTRA_SYMBOL = "stock-symbol";
    public final static String PARAMS_EXTRA_START_DATE = "start_date";
    public final static String PARAMS_EXTRA_END_DATE = "end_date";

    public SingleStockTaskService(){}

    String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params){

        // Retrieves the stock symbol, start and end dates from params
        String symbol = params.getExtras().getString(PARAMS_EXTRA_SYMBOL);
        String startDate = params.getExtras().getString(PARAMS_EXTRA_START_DATE);
        String endDate = params.getExtras().getString(PARAMS_EXTRA_END_DATE);

        StringBuilder urlStringBuilder = new StringBuilder();

        // Builds the base URL for the Yahoo query
        urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");

        try {
            urlStringBuilder.append(
                    URLEncoder.encode(
                            "select * from yahoo.finance.historicaldata where symbol=\"" + symbol + "\"" +
                                    " and startDate=\"" + startDate + "\"" +
                                    " and endDate=\"" + endDate + "\"", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString = urlStringBuilder.toString();

        int result = GcmNetworkManager.RESULT_FAILURE;

        try {
            String getResponse = fetchData(urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;

            Log.d(LOG_TAG, "Retrieved historic stock data for stock " + symbol);

            ArrayList<QuoteDataPoint> dataPoints = Utils.historicJsonToDataPoints(getResponse);

            // Sends a Local Broadcast message to Detail Activity
            Intent intent = new Intent(INTENT_STOCK_HISTORY);
            intent.putParcelableArrayListExtra(INTENT_EXTRA_STOCK_HISTORY, dataPoints);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } catch (IOException e){
            e.printStackTrace();
        }

        return result;
    }

}