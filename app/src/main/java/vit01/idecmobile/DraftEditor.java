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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.util.ArrayList;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.DraftMessage;
import vit01.idecmobile.Core.DraftsValidator;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.Sender;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;

public class DraftEditor extends AppCompatActivity {
    Spinner compose_stations;
    TextInputEditText compose_echoarea, compose_to, compose_subj, compose_repto, compose_msg;
    DraftMessage message;
    File fileToSave;
    ArrayList<String> station_names = new ArrayList<>();
    ArrayAdapter<String> spinnerAdapter;
    String generatedHash = null;
    int nodeindex = 0;
    String outbox_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_editor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, "Написать");

        getControls();
        Intent incoming = getIntent();

        ExternalStorage.initStorage();

        nodeindex = incoming.getIntExtra("nodeindex", 0);
        outbox_id = Config.values.stations.get(nodeindex).outbox_storage_id;

        String task = incoming.getStringExtra("task");

        switch (task) {
            case "new_in_echo":
                message = new DraftMessage();
                message.echo = incoming.getStringExtra("echoarea");
                fileToSave = ExternalStorage.newMessage(outbox_id, message);
                break;
            case "new_answer":
                message = new DraftMessage();
                IIMessage to_which = (IIMessage) incoming.getSerializableExtra("message");
                message.echo = to_which.echo;
                message.to = to_which.from;
                message.subj = SimpleFunctions.subjAnswer(to_which.subj);
                message.repto = to_which.id;

                if (incoming.getBooleanExtra("quote", false)) {
                    message.msg = SimpleFunctions.quoteAnswer(to_which.msg, message.to, Config.values.oldQuote);
                }
                fileToSave = ExternalStorage.newMessage(outbox_id, message);
                break;
            case "edit_existing":
                fileToSave = (File) incoming.getSerializableExtra("file");
                message = ExternalStorage.readDraft(fileToSave);
                break;
        }

        if (fileToSave == null || message == null) {
            Toast.makeText(DraftEditor.this, "Не удалось создать/открыть файл", Toast.LENGTH_SHORT).show();
            SimpleFunctions.debug("Проблема с созданием/открытием файла!");
            finish();
            return;
        }

        // Если алгоритм сохранил файл, то он сгенерировал и хэш для черновика
        // И этот хэш нам нужен
        if (task.contains("new"))
            generatedHash = DraftsValidator.getLastHash();

        if (!Config.values.defaultEditor) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(fileToSave), "text/plain");

            startActivity(intent);
            finish();
            return;
        }

        installValues();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);

        Context context = getApplicationContext();

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        menu.findItem(R.id.action_compose_send).setIcon(new IconicsDrawable
                (context, GoogleMaterial.Icon.gmd_send).actionBar().color(iconColor));
        menu.findItem(R.id.action_compose_delete).setIcon(new IconicsDrawable
                (context, GoogleMaterial.Icon.gmd_delete).actionBar().color(iconColor));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_compose_send) {
            fetchValues();
            saveMessage();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Boolean sent = Sender.sendOneMessage(DraftEditor.this,
                            Config.values.stations.get(nodeindex), fileToSave, true);

                    assert sent != null;
                    DraftsValidator.deleteHash(generatedHash);
                    final String statusText = (sent) ? "Сообщение отправлено" : "Ошибка отправки!";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), statusText, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();

            finish();
        } else if (id == R.id.action_compose_delete) {
            Toast.makeText(DraftEditor.this, "Удаляем черновик", Toast.LENGTH_SHORT).show();
            boolean r = fileToSave.delete();
            if (!r) {
                Toast.makeText(DraftEditor.this, "Удалить не получилось!", Toast.LENGTH_SHORT).show();
            } else fileToSave = null;
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        fetchValues();
        saveMessage();
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void getControls() {
        compose_stations = (Spinner) findViewById(R.id.compose_stations);
        compose_echoarea = (TextInputEditText) findViewById(R.id.compose_echoarea);
        compose_to = (TextInputEditText) findViewById(R.id.compose_to);
        compose_subj = (TextInputEditText) findViewById(R.id.compose_subj);
        compose_repto = (TextInputEditText) findViewById(R.id.compose_repto);
        compose_msg = (TextInputEditText) findViewById(R.id.compose_msg);

        compose_stations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (nodeindex != position) {
                    String secondOutbox_id = Config.values.stations.get(position).outbox_storage_id;
                    File newDirectory = ExternalStorage.getStationStorageDir(secondOutbox_id);

                    File newFile = new File(newDirectory, fileToSave.getName());
                    boolean renamed = fileToSave.renameTo(newFile);

                    if (!renamed) {
                        Toast.makeText(DraftEditor.this, "Переместить на другую станцию не получилось!", Toast.LENGTH_SHORT).show();
                    }

                    fileToSave = newFile;
                    nodeindex = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void installValues() {
        for (Station station : Config.values.stations) {
            station_names.add(station.nodename);
        }
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, station_names);
        compose_stations.setAdapter(spinnerAdapter);
        compose_stations.setSelection(nodeindex);

        compose_echoarea.setText(message.echo);
        compose_to.setText(message.to);
        compose_subj.setText(message.subj);
        compose_repto.setText((message.repto != null) ? message.repto : "");
        compose_msg.setText(message.msg);
    }

    public void fetchValues() {
        message.echo = compose_echoarea.getText().toString();
        message.to = compose_to.getText().toString();
        message.subj = compose_subj.getText().toString();

        String repto = compose_repto.getText().toString();
        message.repto = (repto.equals("") ? null : repto);
        message.msg = compose_msg.getText().toString();
    }

    public void saveMessage() {
        boolean result = ExternalStorage.writeDraftToFile(fileToSave, message.raw());
        if (!result) {
            SimpleFunctions.debug("Проблемсы!");
            Toast.makeText(DraftEditor.this, "Файл как-то не сохранён. Сожалею :(", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
