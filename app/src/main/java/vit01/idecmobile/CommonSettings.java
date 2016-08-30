package vit01.idecmobile;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class CommonSettings extends AppCompatActivity {
    CheckBox defaultEditor, firstrun, useProxy;
    EditText messages_per_fetch, connTimeout, echoEdit;
    ListView offline_echoes;
    int echoPosition;
    AlertDialog alertDialog;
    ArrayAdapter offline_echoes_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_settings);
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
        offline_echoes = (ListView) findViewById(R.id.settings_offline_echoareas_listview);
        echoEdit = new EditText(this);

        alertDialog = new AlertDialog.Builder(CommonSettings.this, R.style.AppTheme)
                .setTitle("Правка эхоконференции")
                .setView(echoEdit)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String gotText = echoEdit.getText().toString();
                        if (!gotText.equals("")) {
                            Config.values.offlineEchoareas.set(echoPosition, gotText);
                            offline_echoes_adapter.notifyDataSetChanged();
                        }
                    }
                }).create();
    }

    protected void installValues() {
        defaultEditor.setChecked(Config.values.defaultEditor);
        firstrun.setChecked(Config.values.firstRun);
        useProxy.setChecked(Config.values.useProxy);

        messages_per_fetch.setText(
                String.valueOf(Config.values.oneRequestLimit), TextView.BufferType.EDITABLE);
        connTimeout.setText(
                String.valueOf(Config.values.connectionTimeout), TextView.BufferType.EDITABLE);
        offline_echoes_adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                        Config.values.offlineEchoareas);

        offline_echoes.setAdapter(offline_echoes_adapter);
        offline_echoes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                echoPosition = position;
                echoEdit.setText(((TextView) view).getText());
                alertDialog.show();
                return true;
            }
        });
    }

    protected void fetchValues() {
        Config.values.defaultEditor = defaultEditor.isChecked();
        Config.values.firstRun = firstrun.isChecked();
        Config.values.useProxy = useProxy.isChecked();

        Config.values.oneRequestLimit = Integer.parseInt(messages_per_fetch.getText().toString());
        Config.values.connectionTimeout = Integer.parseInt(connTimeout.getText().toString());
    }

    @Override
    protected void onStop() {
        super.onStop();
        fetchValues();
        Config.writeConfig(this);
    }
}
