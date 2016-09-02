package vit01.idecmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class CommonSettings extends AppCompatActivity {
    CheckBox defaultEditor, firstrun, useProxy;
    EditText messages_per_fetch, connTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки клиента");
        getControls();
        installValues();
    }

    protected void getControls() {
        defaultEditor = (CheckBox) findViewById(R.id.checkBox);
        firstrun = (CheckBox) findViewById(R.id.checkBox2);
        useProxy = (CheckBox) findViewById(R.id.checkBox3);

        messages_per_fetch = (EditText) findViewById(R.id.editText);
        connTimeout = (EditText) findViewById(R.id.editText2);
   }

    protected void installValues() {
        defaultEditor.setChecked(Config.values.defaultEditor);
        firstrun.setChecked(Config.values.firstRun);
        useProxy.setChecked(Config.values.useProxy);

        messages_per_fetch.setText(
                String.valueOf(Config.values.oneRequestLimit), TextView.BufferType.EDITABLE);
        connTimeout.setText(
                String.valueOf(Config.values.connectionTimeout), TextView.BufferType.EDITABLE);

    }

    protected void fetchValues() {
        Config.values.defaultEditor = defaultEditor.isChecked();
        Config.values.firstRun = firstrun.isChecked();
        Config.values.useProxy = useProxy.isChecked();

        Config.values.oneRequestLimit = Integer.parseInt(messages_per_fetch.getText().toString());
        Config.values.connectionTimeout = Integer.parseInt(connTimeout.getText().toString());
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
    }
}