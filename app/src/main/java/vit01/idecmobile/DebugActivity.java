package vit01.idecmobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.DraftStorage;
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.Sender;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;
import vit01.idecmobile.Core.Station;

public class DebugActivity extends AppCompatActivity {
    ScrollView debugLayout;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Окно отладки");
        debugLayout = (ScrollView) findViewById(R.id.debugLayout);
        textView = (TextView) findViewById(R.id.debug_view);
        textView.setText("");

        SimpleFunctions.debugTaskFinished = false;

        new Thread(new updateDebug()).start();

        Intent intent = getIntent();
        String task = intent.getStringExtra("task");

        if (task.equals("fetch"))
            new Thread(new doFetch()).start();
        else if (task.equals("send"))
            new Thread(new sendMessages()).start();
        else if (task.equals("download_file")) {
            xfile_download xfile_load = new xfile_download();
            xfile_load.station = Config.values.stations.get(intent.getIntExtra("nodeindex", 0));
            xfile_load.filename = intent.getStringExtra("filename");

            new Thread(xfile_load).start();
        } else if (task.equals("truncate_echo")) {
            String echoarea = intent.getStringExtra("echoarea");
            int limit = intent.getIntExtra("limit", 50);

            truncate_echo truncate = new truncate_echo();
            truncate.echoarea = echoarea;
            truncate.limit = limit;

            new Thread(truncate).start();
        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(DebugActivity.this, "Как нехорошо закрывать окно дебага!", Toast.LENGTH_SHORT).show();
    }

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
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
                            textView.append(message);
                            debugLayout.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                });
            }
        }
    }

    class sendMessages implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int sent = 0;

            try {
                DraftStorage.initStorage();
                sent = Sender.sendMessages(getApplicationContext());
                SimpleFunctions.debug("Отправлено сообщений: " + String.valueOf(sent));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
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

    class doFetch implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            Context appContext = getApplicationContext();
            AbstractTransport db = new SqliteTransport(appContext);

            ArrayList<String> fetched = new ArrayList<>();

            try {
                Fetcher fetcher = new Fetcher(db);

                for (Station station : Config.values.stations) {
                    if (!station.fetch_enabled) {
                        SimpleFunctions.debug("skip fetching " + station.nodename);
                        continue;
                    }

                    String xc_id = (station.xc_enable) ?
                            station.outbox_storage_id : null;
                    int ue_limit = (station.advanced_ue) ? station.ue_limit : 0;

                    fetched = fetcher.fetch_messages(appContext,
                            station.address,
                            station.echoareas,
                            xc_id,
                            Config.values.oneRequestLimit,
                            ue_limit,
                            station.pervasive_ue,
                            station.cut_remote_index,
                            Config.values.connectionTimeout
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final String finalFetched = String.valueOf(fetched.size());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Получено сообщений: " + finalFetched, Toast.LENGTH_SHORT).show();
                    }
                });
                finishTask();
            }
        }
    }

    class xfile_download implements Runnable {
        public Station station;
        public String filename;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            boolean success = false;

            DraftStorage.initStorage();
            final File new_file = new File(DraftStorage.rootStorage.getParentFile(), filename);

            try {
                success = Fetcher.xfile_download(getApplicationContext(), station, filename, new_file);
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;

                final boolean finalSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String result = (finalSuccess) ? "Файл загружен в " + new_file.getAbsolutePath() : "Были ошибки";
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                    }
                });
                finishTask();
            }
        }
    }

    class truncate_echo implements Runnable {
        public String echoarea = "no.echo";
        public int limit = 50;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;

            boolean needTruncate = true;

            try {
                AbstractTransport transport = GlobalTransport.transport;
                int countMessages = transport.countMessages(echoarea);

                SimpleFunctions.debug("Выбранный лимит: " + String.valueOf(limit) + " сообщений, исходное количество: " + String.valueOf(countMessages));
                if (countMessages <= limit) needTruncate = false;
                else {
                    int deleteLength = countMessages - limit;
                    SimpleFunctions.debug("Составляется список...");
                    ArrayList<String> deleteThem = transport.getMsgList(echoarea, 0, deleteLength);
                    SimpleFunctions.debug("Удаляем лишние сообщения...");
                    transport.deleteMessages(deleteThem, echoarea);
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;

                final boolean finalNeedTruncate = needTruncate;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String answer = (finalNeedTruncate) ? "Эха подчищена" : "Здесь приемлемое количество сообщений!";
                        Toast.makeText(getApplicationContext(), answer, Toast.LENGTH_SHORT).show();
                    }
                });
                finishTask();
            }
        }
    }
}