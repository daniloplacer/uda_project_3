package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.gcm.TaskParams;

public class SingleStockIntentService extends IntentService {

    private String LOG_TAG = SingleStockIntentService.class.getSimpleName();

    public SingleStockIntentService() {
        super(SingleStockIntentService.class.getName());
    }

    public SingleStockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(LOG_TAG, "Historic Stock Intent Service");

        Bundle args = new Bundle();

        args.putString(SingleStockTaskService.PARAMS_EXTRA_SYMBOL,
                intent.getStringExtra(SingleStockTaskService.PARAMS_EXTRA_SYMBOL));
        args.putString(SingleStockTaskService.PARAMS_EXTRA_START_DATE,
                intent.getStringExtra(SingleStockTaskService.PARAMS_EXTRA_START_DATE));
        args.putString(SingleStockTaskService.PARAMS_EXTRA_END_DATE,
                intent.getStringExtra(SingleStockTaskService.PARAMS_EXTRA_END_DATE));

        // Initiates the task service
        SingleStockTaskService taskService = new SingleStockTaskService();
        taskService.onRunTask(new TaskParams(SingleStockTaskService.PARAMS_TAG, args));

    }
}