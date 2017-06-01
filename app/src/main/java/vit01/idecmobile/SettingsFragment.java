/*
 * Copyright (c) 2017 Boris Timofeev <btimofeev@emunix.org>
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

package vit01.idecmobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import vit01.idecmobile.Core.Config;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals("select_theme")) {
            Toast.makeText(getActivity(), R.string.settings_select_theme_message_restart_app, Toast.LENGTH_SHORT).show();
        }
        if (key.equals("editor")) {
            Config.values.defaultEditor = sharedPreferences.getBoolean(key, true);
        }
        if (key.equals("citation")) {
            Config.values.oldQuote = sharedPreferences.getBoolean(key, false);
        }
        if (key.equals("pull_to_refresh")) {
            Config.values.swipeToFetch = sharedPreferences.getBoolean(key, true);
        }
        if (key.equals("hide_toolbar")) {
            Config.values.hide_toolbar_when_scrolling = sharedPreferences.getBoolean(key, false);
        }
        if (key.equals("open_last_read_message")) {
            Config.values.disableMsglist = sharedPreferences.getBoolean(key, true);
        }
        if (key.equals("open_unread_after_fetch")) {
            Config.values.openUnreadAfterFetch = sharedPreferences.getBoolean(key, false);
        }
        if (key.equals("sort_by_date")) {
            Config.values.sortByDate = sharedPreferences.getBoolean(key, true);
        }
        if (key.equals("carbon_names")) {
            Config.values.carbon_to = sharedPreferences.getString(key, "All");
        }
        if (key.equals("carbon_size")) {
            Config.values.carbon_limit = sharedPreferences.getInt(key, 50);
        }
        if (key.equals("notifications")) {
            Config.values.notificationsEnabled = sharedPreferences.getBoolean(key, false);
        }
        if (key.equals("notification_freq")) {
            Config.values.notifyFireDuration = sharedPreferences.getInt(key, 15);
        }
        if (key.equals("notification_vibrate")) {
            Config.values.notificationsVibrate = sharedPreferences.getBoolean(key, true);
        }
        if (key.equals("network_timeout")) {
            int timeout;
            try {
                timeout = Integer.parseInt(sharedPreferences.getString(key, "20"));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Вы ввели некорректное число", Toast.LENGTH_SHORT).show();
                return;
            }
            Config.values.connectionTimeout = timeout;  // проверку на отрицательное число и ноль следует делать в фильтре
        }
        if (key.equals("messages_per_request")) {
            Config.values.oneRequestLimit = sharedPreferences.getInt(key, 20);
        }
        if (key.equals("proxy")) {
            Config.values.useProxy = sharedPreferences.getBoolean(key, false);
        }
        if (key.equals("proxy_address")) {
            Config.values.proxyAddress = sharedPreferences.getString(key, "127.0.0.1:8118");
        }
        if (key.equals("tor")) {
            Config.values.useTor = sharedPreferences.getBoolean(key, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
