package com.example.myapplication.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HydrationNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("BUTTON_1_ACTION".equals(intent.getAction())) {

        } else if ("BUTTON_2_ACTION".equals(intent.getAction())) {

        }
    }
}