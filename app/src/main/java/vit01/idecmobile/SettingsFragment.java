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
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import vit01.idecmobile.prefs.Config;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // Никогда не запускать здесь sharedPreferences.edit(), иначе будет
        // цикл с бесконечной рекурсией!

        switch (key) {
            case "application_theme":
                Toast.makeText(getActivity(), R.string.settings_select_theme_message_restart_app, Toast.LENGTH_SHORT).show();
                break;
            case "connection_timeout":
                int timeout = Config.values.connectionTimeout;

                if (timeout <= 0) {
                    Toast.makeText(getActivity(), "Вы ввели некорректное число", Toast.LENGTH_SHORT).show();
                    Config.values.connectionTimeout = Config.default_values.connectionTimeout;
                    // проверку на отрицательное число и ноль следует делать в фильтре
                }

                break;
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
        SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }
}
