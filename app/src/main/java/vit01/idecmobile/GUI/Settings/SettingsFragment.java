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

package vit01.idecmobile.GUI.Settings;

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
import android.widget.Toast;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.GlobalConfig;
import vit01.idecmobile.Core.Network;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.GUI.Files.FileChooserActivity;
import vit01.idecmobile.R;
import vit01.idecmobile.notify.AlarmService;
import vit01.idecmobile.prefs.Config;

import static android.app.Activity.RESULT_OK;

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

    public Preference.OnPreferenceClickListener configListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            final Activity activity = getActivity();
            switch (preference.getKey()) {
                case "config_import":
                    startActivityForResult(new Intent(activity, FileChooserActivity.class), 3);
                    break;
                case "config_export":
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Config.writeConfig(activity);
                            ExternalStorage.initStorage();
                            String result;
                            try {
                                File toExport = new File(ExternalStorage.rootStorage.getParentFile(),
                                        "idecConfig_" + String.valueOf(System.currentTimeMillis()) + ".obj");
                                if (!toExport.exists() && !toExport.createNewFile())
                                    throw new IOException(getString(R.string.create_file_error) + " " + toExport.getAbsolutePath());

                                FileOutputStream os = new FileOutputStream(toExport);
                                ObjectOutputStream oos = new ObjectOutputStream(os);
                                oos.writeObject(Config.values);
                                oos.close();
                                os.close();

                                result = getString(R.string.config_saved) + " " + toExport.getAbsolutePath();
                            } catch (Exception e) {
                                e.printStackTrace();
                                SimpleFunctions.debug(e.toString());
                                result = getString(R.string.error_formatted, e.toString());
                            }

                            final String finalResult = result;
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(activity).setMessage(finalResult).show();
                                }
                            });
                        }
                    }).start();
                    break;
            }
            return true;
        }
    };

    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        findPreference("connection_timeout").setOnPreferenceClickListener(integerListener);
        findPreference("carbon_limit").setOnPreferenceClickListener(integerListener);
        findPreference("notify_fire_duration").setOnPreferenceClickListener(integerListener);
        findPreference("one_request_limit").setOnPreferenceClickListener(integerListener);

        findPreference("config_import").setOnPreferenceClickListener(configListener);
        findPreference("config_export").setOnPreferenceClickListener(configListener);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            final File file = (File) data.getSerializableExtra("selected_file");
            final Activity activity = getActivity();
            Toast.makeText(activity, getString(R.string.file_chosen, file.getAbsolutePath()), Toast.LENGTH_SHORT).show();

            if (requestCode == 3) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String result;
                        if (!file.exists() || !file.canRead())
                            result = getString(R.string.no_file_warning);
                        else {
                            if (file.length() > (1024 * 1024))
                                result = getString(R.string.config_very_big);
                            else {
                                try {
                                    FileInputStream is = new FileInputStream(file);
                                    ObjectInputStream ois = new ObjectInputStream(is);
                                    Config.values = (GlobalConfig) ois.readObject();
                                    ois.close();
                                    is.close();
                                    Config.configUpdate(activity);
                                    result = getString(R.string.done);
                                } catch (Exception e) {
                                    result = getString(R.string.config_not_found) + ": " + e.toString();
                                    SimpleFunctions.debug(result);
                                    e.printStackTrace();

                                    Config.loadConfig(activity.getApplicationContext());
                                }
                                Config.writeConfig(activity.getApplicationContext());
                            }
                        }
                        final String finalResult = result;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, finalResult, Toast.LENGTH_SHORT).show();
                                activity.finish();
                            }
                        });
                    }
                }).start();
            }
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
