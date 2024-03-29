/*
 * Copyright (c) 2016-2022 Viktor Fedenyov <me@alicorn.tk> <https://alicorn.tk>
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

package vit01.idecmobile.GUI.Drafts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;

import vit01.idecmobile.Core.DraftMessage;
import vit01.idecmobile.Core.DraftsValidator;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.Sender;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class DraftEditor extends AppCompatActivity {
    Spinner compose_stations;
    TextInputEditText compose_echoarea, compose_to, compose_subj, compose_repto, compose_msg;
    DraftMessage message;
    File fileToSave;
    ArrayAdapter<String> spinnerAdapter;
    String generatedHash = null;
    int nodeindex = 0;
    String outbox_id;
    boolean cancelSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, getString(R.string.action_compose));

        getControls();
        Intent incoming = getIntent();

        ExternalStorage.initStorage();

        String action = incoming.getAction();
        if (Intent.ACTION_PROCESS_TEXT.equals(action) || Intent.ACTION_SEND.equals(action)) {
            // юзверь выбрал поделиться текстом через контекстное меню
            incoming.putExtra("echoarea", "");
            incoming.putExtra("task", "new_in_echo");
        }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && incoming.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            // если сообщение заполнено заранее, подставляем текст
            message.msg = incoming.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
        }

        if (incoming.hasExtra(Intent.EXTRA_TEXT)) {
            message.msg = incoming.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (incoming.hasExtra(Intent.EXTRA_SUBJECT)) {
            message.msg = incoming.getStringExtra(Intent.EXTRA_SUBJECT);
        }

        if (fileToSave == null || message == null) {
            Toast.makeText(DraftEditor.this, R.string.open_create_file_error, Toast.LENGTH_SHORT).show();
            SimpleFunctions.debug("file creation problem");
            finish();
            return;
        }

        if (task.contains("new")) {
            // замещаем intent, чтобы при повороте экрана не создавалось
            // заново сообщение
            Intent override = new Intent();
            override.putExtra("task", "edit_existing");
            override.putExtra("file", fileToSave);
            setIntent(override);
        }

        // Если алгоритм сохранил файл, то он сгенерировал и хэш для черновика
        // И этот хэш нам нужен
        if (task.contains("new"))
            generatedHash = DraftsValidator.getLastHash();

        if (!Config.values.defaultEditor) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(fileToSave), "text/plain");

            try {
                startActivity(intent);
                finish();
                return;
            }
            catch (Exception e) {
                Toast.makeText(DraftEditor.this, R.string.draft_external_editor_error, Toast.LENGTH_SHORT).show();
            }
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
            cancelSaving = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Boolean sent = Sender.sendOneMessage(DraftEditor.this,
                            Config.values.stations.get(nodeindex), fileToSave, true);

                    assert sent != null;
                    DraftsValidator.deleteHash(generatedHash);
                    final int statusText = (sent) ? R.string.message_sent : R.string.error_sending;
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
            cancelSaving = true;
            Toast.makeText(DraftEditor.this, R.string.delete_draft, Toast.LENGTH_SHORT).show();
            boolean r = fileToSave.delete();
            if (!r) {
                Toast.makeText(DraftEditor.this, R.string.deletion_error, Toast.LENGTH_SHORT).show();
            } else fileToSave = null;
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void getControls() {
        compose_stations = findViewById(R.id.compose_stations);
        compose_echoarea = findViewById(R.id.compose_echoarea);
        compose_to = findViewById(R.id.compose_to);
        compose_subj = findViewById(R.id.compose_subj);
        compose_repto = findViewById(R.id.compose_repto);
        compose_msg = findViewById(R.id.compose_msg);

        compose_stations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (nodeindex != position) {
                    String secondOutbox_id = Config.values.stations.get(position).outbox_storage_id;
                    File newDirectory = ExternalStorage.getStationStorageDir(secondOutbox_id);

                    File newFile = new File(newDirectory, fileToSave.getName());
                    boolean renamed = fileToSave.renameTo(newFile);

                    if (!renamed) {
                        Toast.makeText(DraftEditor.this, R.string.error_moving_to_station, Toast.LENGTH_SHORT).show();
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
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                IDECFunctions.getStationsNames());
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
            SimpleFunctions.debug(getString(R.string.error));
            Toast.makeText(DraftEditor.this, R.string.unable_to_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        fetchValues();
        if (!cancelSaving) saveMessage();
        super.onPause();
    }
}
