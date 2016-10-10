package vit01.idecmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.notify.AlarmService;

public class CommonSettings extends AppCompatActivity {
    CheckBox defaultEditor, firstrun, useProxy, oldQuote, notifyEnabled, notifyVibrate;
    EditText messages_per_fetch, connTimeout, carbon_usernames, carbon_limit, notifyInterval;
    Intent alarmIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        notifyEnabled = (CheckBox) findViewById(R.id.notifications_enabled);
        notifyVibrate = (CheckBox) findViewById(R.id.notification_vibrate);

        messages_per_fetch = (EditText) findViewById(R.id.editText);
        connTimeout = (EditText) findViewById(R.id.editText2);
        carbon_usernames = (EditText) findViewById(R.id.editText3);
        carbon_limit = (EditText) findViewById(R.id.editText4);
        notifyInterval = (EditText) findViewById(R.id.notifications_time_interval);

        alarmIntent = new Intent(this, AlarmService.class);
        notifyEnabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchValues();
                Config.writeConfig(getApplicationContext());
                startService(alarmIntent);
            }
        });
    }

    protected void installValues() {
        defaultEditor.setChecked(Config.values.defaultEditor);
        firstrun.setChecked(Config.values.firstRun);
        useProxy.setChecked(Config.values.useProxy);
        oldQuote.setChecked(Config.values.oldQuote);
        notifyEnabled.setChecked(Config.values.notificationsEnabled);
        notifyVibrate.setChecked(Config.values.notificationsVibrate);

        messages_per_fetch.setText(
                String.valueOf(Config.values.oneRequestLimit), TextView.BufferType.EDITABLE);
        connTimeout.setText(
                String.valueOf(Config.values.connectionTimeout), TextView.BufferType.EDITABLE);
        carbon_usernames.setText(Config.values.carbon_to, TextView.BufferType.EDITABLE);
        carbon_limit.setText(
                String.valueOf(Config.values.carbon_limit), TextView.BufferType.EDITABLE);
        notifyInterval.setText(
                String.valueOf(Config.values.notifyFireDuration), TextView.BufferType.EDITABLE);
    }

    protected void fetchValues() {
        Config.values.defaultEditor = defaultEditor.isChecked();
        Config.values.firstRun = firstrun.isChecked();
        Config.values.useProxy = useProxy.isChecked();
        Config.values.oldQuote = oldQuote.isChecked();
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
    }

    public void openEchoEdit(View view) {
        Intent intent = new Intent(CommonSettings.this, ListEditActivity.class);
        intent.putExtra("type", "offline");
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        fetchValues();
        Config.writeConfig(this);
        startService(alarmIntent);
    }
}