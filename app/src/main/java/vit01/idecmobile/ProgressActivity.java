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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

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
    ImageView errorView;
    Button viewLog;
    String debugLog = "";
    boolean closeWindow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        initUI();

        Intent gotIntent = getIntent();
        String task = gotIntent.getStringExtra("task");
        chooseTitle(task);

        SimpleFunctions.debugTaskFinished = false;
        SimpleFunctions.debugMessages.clear();
        new Thread(new updateDebug()).start();

        switch (task) {
            case "fetch":
                progressBar.setIndeterminate(true);
                new Thread(new doFetch()).start();
                // Continue, copy code from DebugActivity

                break;
            case "send":
                progressBar.setIndeterminate(false);
                new Thread(new sendMessages()).start();
                // write code here
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!SimpleFunctions.debugTaskFinished) {
            Toast.makeText(ProgressActivity.this, "Потом окно закроешь!", Toast.LENGTH_SHORT).show();
        } else if (!closeWindow) finish();
    }

    @Override
    public void onDestroy() {
        SimpleFunctions.debugTaskFinished = true;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_progress);
        initUI();
        chooseTitle(getIntent().getStringExtra("task"));
        if (SimpleFunctions.debugTaskFinished && !closeWindow) {
            errorHappened();
        }
    }

    public void initUI() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        info = (TextView) findViewById(R.id.progress_status_text);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        errorView = (ImageView) findViewById(R.id.error_view);
        viewLog = (Button) findViewById(R.id.progress_watch_log);

        viewLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                while (SimpleFunctions.debugMessages.size() > 0) {
                    debugLog += SimpleFunctions.debugMessages.remove() + "\n";
                }
                new AlertDialog.Builder(ProgressActivity.this)
                        .setMessage(debugLog)
                        .setTitle("Debug Log")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        errorView.setImageDrawable(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_warning).color(
                SimpleFunctions.colorFromTheme(this, android.R.attr.textColorSecondary)).sizeDp(100));
    }

    public void chooseTitle(String task) {
        String title;
        switch (task) {
            case "fetch":
                title = "Скачивание сообщений";
                break;
            case "send":
                title = "Отправка почты";
                break;
            default:
                title = "<null>";
                break;
        }
        SimpleFunctions.setActivityTitle(this, title);
    }

    public void finishTask() {
        SimpleFunctions.debugTaskFinished = true;

        SimpleFunctions.prettyDebugMessages.clear();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (closeWindow) finish();
            }
        });
    }

    public void errorHappened() {
        info.setText("Выполнено с ошибками");
        viewLog.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        closeWindow = false;
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
                final int finalFetched = fetchedCount;
                final int finalErrorFlag = error_flag;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "";
                        if (finalErrorFlag > 0) {
                            errorHappened();

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
            int sent = 0;

            try {
                sent = Sender.sendMessages(getApplicationContext());
                SimpleFunctions.debug("Отправлено сообщений: " + String.valueOf(sent));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());

                errorHappened();
            } finally {
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