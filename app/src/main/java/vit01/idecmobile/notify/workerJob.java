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

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import java.util.Enumeration;
import java.util.Hashtable;

import vit01.idecmobile.Core.Network;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class workerJob extends BroadcastReceiver {
    public static Hashtable<String, Integer> lastDifference = null;
    long[] vibrate_pattern = {1000, 1000};
    boolean vibrate = false;

    public static int getNotificationID() {
        return 42;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (intent.getAction() != null && intent.getAction().equals("notify_cancel")) {
            lastDifference = null;
            return;
        }

        if (Config.values == null) Config.loadConfig(context);

        vibrate = Config.values.notificationsVibrate;

        if (lastDifference == null) lastDifference = new Hashtable<>();

        for (Station station : Config.values.stations) {
            if (station.fetch_enabled && station.xc_enable)
                addToHashTable(lastDifference, pass_to_xc_api(context, station));
        }
        handleResult(context, lastDifference);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void Show_Notification(Context context, String title, String text, boolean big_text) {
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher_notify)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true);

        if (vibrate) {
            mBuilder.setVibrate(vibrate_pattern);
        } else mBuilder.setVibrate(null);

        if (big_text) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        }

        Intent resultIntent = new Intent(context, vit01.idecmobile.MainActivity.class);
        resultIntent.putExtra("task", "fetch");

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(vit01.idecmobile.MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent navigateIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(navigateIntent);

        Intent deleteIntent = new Intent(context, workerJob.class);
        deleteIntent.setAction("notify_cancel");
        mBuilder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(getNotificationID(), mBuilder.build());
    }

    public Hashtable<String, Integer> pass_to_xc_api(Context context, Station station) {
        String server_answer = Network.getFile(context,
                station.address + "x/c/" + TextUtils.join("/", station.echoareas), null, 10);

        if (server_answer == null) {
            SimpleFunctions.debug("Error getting server info (xc api)");
            return null;
        }

        String xc_cell_name = "xc_tmp_" + station.outbox_storage_id;

        String local_xc_data = SimpleFunctions.read_internal_file(context, xc_cell_name);
        SimpleFunctions.write_internal_file(context, xc_cell_name, server_answer);

        if (local_xc_data.equals("")) {
            // Если получили данные в первый раз, то отслеживание не нужно: выходим
            return null;
        }

        String[] local_xc_lines = local_xc_data.split("\n");
        String[] remote_xc_lines = server_answer.split("\n");

        if (local_xc_lines.length != remote_xc_lines.length) {
            return null;
            // Значит пользователь просто обновил список эх. Продолжать не следует
        }

        Hashtable<String, Integer> remote_xc_dict = new Hashtable<>();
        Hashtable<String, Integer> local_xc_dict = new Hashtable<>();

        try {
            xc_parse_values(remote_xc_dict, remote_xc_lines);
            xc_parse_values(local_xc_dict, local_xc_lines);
        } catch (Exception e) {
            SimpleFunctions.debug("Exception: " + e);
            return null;
        }

        return assoc_difference(local_xc_dict, remote_xc_dict);
    }

    private void handleResult(Context context, Hashtable difference) {
        if (difference.size() > 0) {
            Enumeration result_echoareas = difference.keys();

            Integer total = 0;
            String notification_text = "";

            while (result_echoareas.hasMoreElements()) {
                String echoarea = result_echoareas.nextElement().toString();
                Integer next = (Integer) difference.get(echoarea);

                total += next;
                notification_text += echoarea + ": " + String.valueOf(next) + "\n";
            }

            if (notification_text.length() > 0) // костыль с последним переносом строки
                notification_text = notification_text.substring(0, notification_text.length() - 1);

            Show_Notification(context, context.getString(R.string.new_messages, total),
                    notification_text, true);
        }
    }

    private void xc_parse_values(Hashtable<String, Integer> htable, String[] lines) {
        for (String line : lines) {
            String[] pieces = line.split(":");
            if (pieces.length < 2) continue;

            int value = Integer.parseInt(pieces[1]);

            htable.put(pieces[0], value);
        }
    }

    private Hashtable<String, Integer> assoc_difference(Hashtable<String, Integer> first, Hashtable<String, Integer> second) {
        Hashtable<String, Integer> result = new Hashtable<>();

        Enumeration keys_local = first.keys();

        while (keys_local.hasMoreElements()) {
            String key_string = keys_local.nextElement().toString();

            Integer firstValue = first.get(key_string);
            if (!second.containsKey(key_string)) continue;
            Integer secondValue = second.get(key_string);

            if (secondValue == null) continue;

            if (secondValue > firstValue) {
                result.put(key_string, secondValue - firstValue);
            }
        }

        return result;
    }

    public void addToHashTable(Hashtable<String, Integer> first, Hashtable<String, Integer> second) {
        if (first == null || second == null) return;
        Enumeration<String> secondKeys = second.keys();

        while (secondKeys.hasMoreElements()) {
            String current = secondKeys.nextElement();
            if (first.containsKey(current)) {
                first.put(current, first.get(current) + second.get(current));
            } else first.put(current, second.get(current));
        }
    }
}