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

package vit01.idecmobile;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.Network;
import vit01.idecmobile.notify.AlarmService;

public class CommonSettings extends AppCompatActivity {
    CheckBox defaultEditor, firstrun, useProxy, oldQuote, notifyEnabled, notifyVibrate, useTor, swipeToFetch, disable_msglist;
    EditText messages_per_fetch, connTimeout, carbon_usernames, carbon_limit, notifyInterval, proxyAddress;
    Spinner selected_theme;
    Intent alarmIntent;
    Resources res;
    List<String> realThemeNames; // те имена, которые пишутся в конфиг
    boolean showThemeChangeAlert = true;
    AdapterView.OnItemSelectedListener spinnerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки клиента");
        getControls();
        installValues();
    }

    protected void getControls() {
        defaultEditor = (CheckBox) findViewById(R.id.checkBox);
        firstrun = (CheckBox) findViewById(R.id.checkBox2);
        useProxy = (CheckBox) findViewById(R.id.checkBox3);
        oldQuote = (CheckBox) findViewById(R.id.old_quote);
        swipeToFetch = (CheckBox) findViewById(R.id.swipe_to_fetch_enable);
        disable_msglist = (CheckBox) findViewById(R.id.echoview_disable_messages_list);
        notifyEnabled = (CheckBox) findViewById(R.id.notifications_enabled);
        notifyVibrate = (CheckBox) findViewById(R.id.notification_vibrate);
        useTor = (CheckBox) findViewById(R.id.useTor);

        messages_per_fetch = (EditText) findViewById(R.id.editText);
        connTimeout = (EditText) findViewById(R.id.editText2);
        carbon_usernames = (EditText) findViewById(R.id.editText3);
        carbon_limit = (EditText) findViewById(R.id.editText4);
        notifyInterval = (EditText) findViewById(R.id.notifications_time_interval);
        proxyAddress = (EditText) findViewById(R.id.proxy_address);

        res = getResources();
        realThemeNames = Arrays.asList(res.getStringArray(R.array.themes));

        ArrayAdapter themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, res.getStringArray(R.array.themes_names));
        selected_theme = (Spinner) findViewById(R.id.selected_theme);
        selected_theme.setAdapter(themeAdapter);

        alarmIntent = new Intent(this, AlarmService.class);
        notifyEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchValues();
                Config.writeConfig(getApplicationContext());
                startService(alarmIntent);
            }
        });

        useTor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useTor.isChecked()) {
                    Context context = getApplicationContext();
                    if (!OrbotHelper.isOrbotInstalled(context)) {
                        Intent intent = OrbotHelper.getOrbotInstallIntent(context);
                        startActivity(intent);
                    } else {
                        if (useProxy.isChecked() && !OrbotHelper.isOrbotRunning(context)) {
                            Intent intent = OrbotHelper.getShowOrbotStartIntent();
                            startActivity(intent);
                        }
                    }
                }
            }
        });

        spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!showThemeChangeAlert) return; // два раза предупреждение не показываем

                new AlertDialog.Builder(CommonSettings.this)
                        .setTitle("Предупреждение")
                        .setMessage("Для применения тем требуется перезапуск приложения!")
                        .setPositiveButton("Ясно", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showThemeChangeAlert = false;
                            }
                        })
                        .show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    protected void installValues() {
        defaultEditor.setChecked(Config.values.defaultEditor);
        firstrun.setChecked(Config.values.firstRun);
        useProxy.setChecked(Config.values.useProxy);
        oldQuote.setChecked(Config.values.oldQuote);
        swipeToFetch.setChecked(Config.values.swipeToFetch);
        disable_msglist.setChecked(Config.values.disableMsglist);
        notifyEnabled.setChecked(Config.values.notificationsEnabled);
        notifyVibrate.setChecked(Config.values.notificationsVibrate);
        useTor.setChecked(Config.values.useTor);

        messages_per_fetch.setText(
                String.valueOf(Config.values.oneRequestLimit), TextView.BufferType.EDITABLE);
        connTimeout.setText(
                String.valueOf(Config.values.connectionTimeout), TextView.BufferType.EDITABLE);
        carbon_usernames.setText(Config.values.carbon_to, TextView.BufferType.EDITABLE);
        carbon_limit.setText(
                String.valueOf(Config.values.carbon_limit), TextView.BufferType.EDITABLE);
        notifyInterval.setText(
                String.valueOf(Config.values.notifyFireDuration), TextView.BufferType.EDITABLE);
        proxyAddress.setText(Config.values.proxyAddress);

        int themeIndex = realThemeNames.indexOf(Config.values.applicationTheme);
        if (themeIndex < 0) themeIndex = 0;

        selected_theme.setSelection(themeIndex, false);

        if (selected_theme.getOnItemSelectedListener() == null)
            selected_theme.setOnItemSelectedListener(spinnerListener);
    }

    protected void fetchValues() {
        Config.values.defaultEditor = defaultEditor.isChecked();
        Config.values.firstRun = firstrun.isChecked();
        Config.values.useProxy = useProxy.isChecked();
        Config.values.oldQuote = oldQuote.isChecked();
        Config.values.swipeToFetch = swipeToFetch.isChecked();
        Config.values.disableMsglist = disable_msglist.isChecked();
        Config.values.notificationsEnabled = notifyEnabled.isChecked();
        Config.values.notificationsVibrate = notifyVibrate.isChecked();

        Config.values.oneRequestLimit = Integer.parseInt(messages_per_fetch.getText().toString());
        Config.values.connectionTimeout = Integer.parseInt(connTimeout.getText().toString());
        Config.values.carbon_to = carbon_usernames.getText().toString();
        Config.values.carbon_limit = Integer.parseInt(carbon_limit.getText().toString());

        int notifyLimit = Integer.parseInt(notifyInterval.getText().toString());
        if (notifyLimit <= 0)
            Toast.makeText(CommonSettings.this, "Чё за дрянь ты написал в интервал для уведомлений?", Toast.LENGTH_SHORT).show();
        else Config.values.notifyFireDuration = notifyLimit;

        Config.values.proxyAddress = proxyAddress.getText().toString();
        Config.values.proxyType = 1; // всегда HTTP-прокси
        Config.values.useTor = useTor.isChecked();

        int themeIndex = selected_theme.getSelectedItemPosition();
        Config.values.applicationTheme = realThemeNames.get(themeIndex);
    }

    public void openEchoEdit(View view) {
        Intent intent = new Intent(CommonSettings.this, ListEditActivity.class);
        intent.putExtra("type", "offline");
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        fetchValues();
        Config.writeConfig(this);
        startService(alarmIntent);
        Network.proxy = null; // сбрасываем значение, чтобы оно распарсилось и пересчиталось заново
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}