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
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.Sender;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.prefs.Config;

public class ProgressActivity extends AppCompatActivity {
    TextView info;
    ProgressBar progressBar;
    Button viewLog;
    String debugLog = "";
    boolean closeWindow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        info = (TextView) findViewById(R.id.progress_status_text);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        viewLog = (Button) findViewById(R.id.progress_watch_log);

        viewLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(ProgressActivity.this)
                        .setMessage(debugLog)
                        .setTitle("Debug Log")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        Intent gotIntent = getIntent();
        String task = gotIntent.getStringExtra("task");

        new Thread(new updateDebug()).start();

        switch (task) {
            case "fetch":
                SimpleFunctions.setActivityTitle(this, "Скачивание сообщений");
                progressBar.setIndeterminate(true);
                new Thread(new doFetch()).start();
                // Continue, copy code from DebugActivity

                break;
            case "send":
                SimpleFunctions.setActivityTitle(this, "Отправка почты");
                progressBar.setIndeterminate(false);
                new Thread(new sendMessages()).start();
                // write code here
                break;
        }
    }

    /*
    @Override
    public void onBackPressed() {
        Toast.makeText(ProgressActivity.this, "Oh, wait!", Toast.LENGTH_SHORT).show();
    }*/

    @Override
    public void onDestroy() {
        SimpleFunctions.debugTaskFinished = true;
        super.onDestroy();
    }

    public void finishTask() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SimpleFunctions.debugMessages.clear();
        SimpleFunctions.prettyDebugMessages.clear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (closeWindow) finish();
            }
        });
    }


    class updateDebug implements Runnable {
        @Override
        public void run() {
            while (!SimpleFunctions.debugTaskFinished) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (SimpleFunctions.debugMessages.size() > 0) {
                            String message = SimpleFunctions.debugMessages.remove() + "\n";
                            debugLog += message;
                        }

                        if (SimpleFunctions.prettyDebugMessages.size() > 0) {
                            info.setText(SimpleFunctions.prettyDebugMessages.remove());
                        }
                    }
                });
            }
        }
    }

    class doFetch implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            Context appContext = getApplicationContext();

            ArrayList<String> fetched;
            int fetchedCount = 0;
            int error_flag = 0;

            try {
                Fetcher fetcher = new Fetcher(GlobalTransport.transport);

                for (Station station : Config.values.stations) {
                    if (!station.fetch_enabled) {
                        SimpleFunctions.debug("skip fetching " + station.nodename);
                        continue;
                    }

                    String xc_id = (station.xc_enable) ?
                            station.outbox_storage_id : null;
                    Integer ue_limit = (station.advanced_ue) ? station.ue_limit : 0;

                    fetched = fetcher.fetch_messages(
                            appContext,
                            station.address,
                            station.echoareas,
                            xc_id,
                            Config.values.oneRequestLimit,
                            ue_limit,
                            station.pervasive_ue,
                            station.cut_remote_index,
                            Config.values.connectionTimeout
                    );

                    if (fetched != null) fetchedCount += fetched.size();
                    else error_flag++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
                error_flag++;
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final int finalFetched = fetchedCount;
                final int finalErrorFlag = error_flag;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "";
                        if (finalErrorFlag > 0) {
                            info.setText("Выполнено с ошибками");
                            viewLog.setVisibility(View.VISIBLE);
                            closeWindow = false;

                            message += "Ошибок: " + String.valueOf(finalErrorFlag) + "\n\n";

                            if (finalFetched == 0)
                                message += "Проблема c загрузкой сообщений\nПроверьте подключение к интернету";
                        } else if (finalFetched == 0) message += "Новых сообщений нет";

                        if (!message.equals(""))
                            Toast.makeText(ProgressActivity.this, message, Toast.LENGTH_SHORT).show();

                        if (finalFetched > 0) {
                            Toast.makeText(getApplicationContext(), "Получено сообщений: " + String.valueOf(finalFetched), Toast.LENGTH_SHORT).show();

                            if (Config.values.openUnreadAfterFetch) {
                                Intent unreadIntent = new Intent(ProgressActivity.this, EchoReaderActivity.class);
                                unreadIntent.putExtra("echoarea", "_unread");
                                startActivity(unreadIntent);
                            }
                        }
                    }
                });
                finishTask();
            }
        }
    }

    class sendMessages implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int sent = 0;

            try {
                sent = Sender.sendMessages(getApplicationContext());
                SimpleFunctions.debug("Отправлено сообщений: " + String.valueOf(sent));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());

                info.setText("Выполнено с ошибками");
                viewLog.setVisibility(View.VISIBLE);
                closeWindow = false;
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final String finalSent = String.valueOf(sent);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Отправлено сообщений: " + finalSent, Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }
    }
}