package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    public static boolean showPercent = true;

    // JSON ATTRIBUTES

    private static final String JSON_QUERY = "query";
    private static final String JSON_COUNT = "count";
    private static final String JSON_RESULTS = "results";

    private static final String JSON_QUOTE_ARRAY = "quote";

    private static final String JSON_SYMBOL = "symbol";
    private static final String JSON_BID_PRICE = "Bid";
    private static final String JSON_PERCENT_CHANGE = "ChangeinPercent";
    private static final String JSON_CHANGE = "Change";

    private static final String JSON_DATE = "Date";
    private static final String JSON_CLOSE_BID = "Close";

    // TODO: 6/15/16 Implement saved state so data is not re-pulled after screen rotation

    public static ArrayList quoteJsonToContentVals(String JSON){
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        ContentProviderOperation opp = null;

        try {
            jsonObject = new JSONObject(JSON);

            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(JSON_QUERY);
                int count = Integer.parseInt(jsonObject.getString(JSON_COUNT));

                // If the request returned more than one item, need to iterate through
                // the array and call buildBatchOperation for each item
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject(JSON_RESULTS)
                            .getJSONObject(JSON_QUOTE_ARRAY);

                    opp = buildBatchOperation(jsonObject);

                    if (opp != null) {
                        batchOperations.add(opp);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(JSON_RESULTS).getJSONArray(JSON_QUOTE_ARRAY);

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            opp = buildBatchOperation(jsonObject);

                            if (opp != null) {
                                batchOperations.add(opp);
                            }
                        }
                    }
                }
            }

        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }

        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice){
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    // Returns null if there the attributes from that JSON item is null
    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);

        try {

            String bid = jsonObject.getString(JSON_BID_PRICE);

            // Not able to retrieve information for that stock symbol
            if (bid.compareTo("null") == 0) {
                return null;
            }

            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(JSON_SYMBOL));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(bid));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(JSON_PERCENT_CHANGE), true));

            String change = jsonObject.getString(JSON_CHANGE);
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));

            builder.withValue(QuoteColumns.ISCURRENT, 1);

            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e){
            e.printStackTrace();
        }

        return builder.build();
    }

    public static ArrayList historicJsonToDataPoints(String JSON){
        ArrayList<QuoteDataPoint> dataPoints = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(JSON);

            if (jsonObject != null && jsonObject.length() != 0) {

                jsonObject = jsonObject.getJSONObject(JSON_QUERY);
                int count = Integer.parseInt(jsonObject.getString(JSON_COUNT));

                // If at least one historic data point was returned
                if (count > 1){

                    JSONArray resultsArray = jsonObject.getJSONObject(JSON_RESULTS).getJSONArray(JSON_QUOTE_ARRAY);

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);

                            String date = jsonObject.getString(JSON_DATE);
                            Float closeBid = Float.parseFloat(jsonObject.getString(JSON_CLOSE_BID));

                            QuoteDataPoint dp = new QuoteDataPoint(date, closeBid);
                            dataPoints.add(dp);
                        }
                    }
                }
            }

        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }

        return dataPoints;
    }



    // Returns today's date minus 1 week in the right format
    public static String getStartDate() {
        Date today = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String startDateString = dateFormat.format(startDate);

        return startDateString;
    }

    // Returns today's date in the right format
    public static String getEndDate() {
        Date today = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String endDateString = dateFormat.format(today);

        return endDateString;
    }


    // Checks whether the device is connected to the internet
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());

    }

}