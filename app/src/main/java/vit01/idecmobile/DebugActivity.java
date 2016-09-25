package vit01.idecmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.DraftStorage;
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.Sender;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;
import vit01.idecmobile.Core.Station;

public class DebugActivity extends AppCompatActivity {
    ScrollView debugLayout;
    TextView textView;
    BroadcastReceiver receiver;
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Окно отладки");
        debugLayout = (ScrollView) findViewById(R.id.debugLayout);
        textView = (TextView) findViewById(R.id.debug_view);
        textView.setText("");

        SimpleFunctions.debugTaskFinished = false;

        filter = new IntentFilter();
        filter.addAction("DebugActivity");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("task").equals("stop")) {
                    finish();
                } else if (intent.getStringExtra("task").equals("addText")) {
                    String data = intent.getStringExtra("data");
                    textView.append(data);
                    debugLayout.fullScroll(View.FOCUS_DOWN);
                } else if (intent.getStringExtra("task").equals("toast")) {
                    String data = intent.getStringExtra("data");
                    Toast.makeText(DebugActivity.this, data, Toast.LENGTH_SHORT).show();
                }
            }
        };
        registerReceiver(receiver, filter);

        new Thread(new updateDebug()).start();

        Intent intent = getIntent();
        String task = intent.getStringExtra("task");

        if (task.equals("fetch"))
            new Thread(new doFetch()).start();
        else if (task.equals("send"))
            new Thread(new sendMessages()).start();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(DebugActivity.this, "Как нехорошо закрывать окно дебага!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        SimpleFunctions.debugTaskFinished = true;
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void takeDebugMessage() {
        if (SimpleFunctions.debugMessages.size() > 0) {
            String message = SimpleFunctions.debugMessages.remove() + "\n";
            Intent myIntent = new Intent("DebugActivity");
            myIntent.putExtra("task", "addText");
            myIntent.putExtra("data", message);
            sendBroadcast(myIntent);
        }
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

                takeDebugMessage();
            }
        }
    }

    class sendMessages implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            Context appContext = getApplicationContext();

            try {
                DraftStorage.initStorage();
                int sent = Sender.sendMessages(getApplicationContext());
                SimpleFunctions.debug("Отправлено сообщений: " + String.valueOf(sent));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                Intent temp = new Intent("DebugActivity");
                temp.putExtra("task", "toast");
                temp.putExtra("data", "Вроде бы, всё!");
                sendBroadcast(temp);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SimpleFunctions.debugMessages.clear();
                Intent intent = new Intent("DebugActivity");
                intent.putExtra("task", "stop");
                sendBroadcast(intent);
            }
        }
    }

    class doFetch implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            Context appContext = getApplicationContext();
            AbstractTransport db = new SqliteTransport(appContext);

            try {
                Fetcher fetcher = new Fetcher(db);

                for (Station station : Config.values.stations) {
                    if (!station.fetch_enabled) {
                        SimpleFunctions.debug("skip fetching " + station.nodename);
                        continue;
                    }

                    String xc_id = (station.xc_enable) ?
                            SimpleFunctions.hsh(station.nodename) : null;
                    int ue_limit = (station.advanced_ue) ? station.ue_limit : 0;

                    fetcher.fetch_messages(appContext,
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
                Intent temp = new Intent("DebugActivity");
                temp.putExtra("task", "toast");
                temp.putExtra("data", "2 секунды, и окно закроется");
                sendBroadcast(temp);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                SimpleFunctions.debugMessages.clear();
                Intent intent = new Intent("DebugActivity");
                intent.putExtra("task", "stop");
                sendBroadcast(intent);
            }
        }
    }
}
