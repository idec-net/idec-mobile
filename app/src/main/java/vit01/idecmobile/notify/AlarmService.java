/*
 * Copyright (c) 2016-2017 Viktor Fedenyov <me@ii-net.tk> <https://ii-net.tk>
 *
 * This file is part of IDEC Mobile.
 *
 * IDEC Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IDEC Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IDEC Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package vit01.idecmobile.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

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
        jobPendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                666, // У секты должны быть соответствующие id уведомлений
                jobIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        SimpleFunctions.debug("IDEC notifications enabled: " + String.valueOf(Config.values.notificationsEnabled));

        if (Config.values.notificationsEnabled) {
            SimpleFunctions.checkTorRunning(getApplicationContext(), false);
            startAlarm();
        } else {
            stopAlarm();
        }
        return START_NOT_STICKY;
    }

    public void startAlarm() {
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000 * 60,       // запустится после одной минуты
                Config.values.notifyFireDuration * 1000 * 60, // интервал обновления
                jobPendingIntent);

        Toast.makeText(AlarmService.this, R.string.notification_service_reload,
                Toast.LENGTH_SHORT).show();
    }

    public void stopAlarm() {
        alarmManager.cancel(jobPendingIntent);
        jobPendingIntent.cancel();
    }
}