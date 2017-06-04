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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import vit01.idecmobile.Core.Network;
import vit01.idecmobile.notify.AlarmService;
import vit01.idecmobile.prefs.Config;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public Preference.OnPreferenceClickListener integerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            int maxValue;

            switch (preference.getKey()) {
                case "connection_timeout":
                    maxValue = 180;
                    break;
                case "one_request_limit":
                    maxValue = 15000;
                    break;
                case "notify_fire_duration":
                    maxValue = 10080;
                    break;
                case "carbon_limit":
                    maxValue = 5000;
                    break;
                default:
                    maxValue = 42;
                    break;
            }

            int value = preference.getSharedPreferences().getInt(preference.getKey(), 1);

            final NumberPicker picker = new NumberPicker(getContext());
            picker.setMinValue(1);
            picker.setMaxValue(maxValue);
            picker.setValue(value);
            picker.setWrapSelectorWheel(false);
            picker.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            final LinearLayout linearLayout = new LinearLayout(getActivity());
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            linearLayout.setGravity(Gravity.CENTER);
            linearLayout.addView(picker);

            new AlertDialog.Builder(getContext())
                    .setTitle(preference.getTitle())
                    .setView(linearLayout)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            picker.clearFocus();
                            int newValue = picker.getValue();
                            SharedPreferences.Editor editor = preference.getSharedPreferences().edit();
                            editor.putInt(preference.getKey(), newValue);
                            editor.apply();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

            return true;
        }
    };

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        findPreference("connection_timeout").setOnPreferenceClickListener(integerListener);
        findPreference("carbon_limit").setOnPreferenceClickListener(integerListener);
        findPreference("notify_fire_duration").setOnPreferenceClickListener(integerListener);
        findPreference("one_request_limit").setOnPreferenceClickListener(integerListener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        // Никогда не запускать здесь sharedPreferences.edit(), иначе будет
        // цикл с бесконечной рекурсией!

        Activity mContext = getActivity();

        switch (key) {
            case "application_theme":
                Config.select_gui_theme();
                Intent intent = mContext.getIntent();
                mContext.finish();
                startActivity(intent);
                break;
            case "use_proxy":
                Network.proxy = null;
                break;
            case "proxy_address":
                Network.proxy = null;
                break;
            case "use_tor":
                boolean useTor = sharedPreferences.getBoolean(key, Config.default_values.useTor);

                if (useTor) {
                    Context context = mContext.getApplicationContext();
                    if (!OrbotHelper.isOrbotInstalled(context)) {
                        startActivity(OrbotHelper.getOrbotInstallIntent(context));
                    } else if (!OrbotHelper.isOrbotRunning(context)) {
                        startActivity(OrbotHelper.getShowOrbotStartIntent());
                    }
                }
                break;
            case "notifications_enabled":
                mContext.startService(new Intent(mContext, AlarmService.class));
                break;
            case "notify_fire_duration":
                mContext.startService(new Intent(mContext, AlarmService.class));
                break;
            case "notifications_vibrate":
                mContext.startService(new Intent(mContext, AlarmService.class));
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
        Config.writeConfig(getActivity()); // Не убирать! Здесь сохраняем конфиг на самом деле!
    }
}
