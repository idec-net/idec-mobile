package vit01.idecmobile.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.SimpleFunctions;

public class AlarmService extends Service {
    AlarmManager alarmManager;
    Intent jobIntent;
    PendingIntent jobPendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        jobIntent = new Intent(this, workerJob.class);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SimpleFunctions.debug("IDEC notifications enabled: " + String.valueOf(Config.values.notificationsEnabled));
        if (Config.values.notificationsEnabled) {
            startAlarm();
        } else {
            stopAlarm();
        }
        return START_NOT_STICKY;
    }

    public void startAlarm() {
        jobPendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                666, // У секты должны быть соответствующие id уведомлений
                jobIntent,
                0);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000 * 60,       // запустится после одной минуты
                Config.values.notifyFireDuration * 1000 * 60, // интервал обновления
                jobPendingIntent);

        Toast.makeText(AlarmService.this, "Уведомления IDECMobile (пере)запущены",
                Toast.LENGTH_SHORT).show();
    }

    public void stopAlarm() {
        alarmManager.cancel(jobPendingIntent);
    }
}