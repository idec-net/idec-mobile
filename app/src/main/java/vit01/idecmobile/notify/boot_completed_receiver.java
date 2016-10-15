package vit01.idecmobile.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import vit01.idecmobile.Core.Config;

public class boot_completed_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Config.loadConfig(context);

            Intent alarmStart = new Intent(context, AlarmService.class);
            context.startService(alarmStart);
        }
    }
}