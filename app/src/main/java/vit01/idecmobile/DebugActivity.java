package vit01.idecmobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
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

        switch (task) {
            case "fetch":
                new Thread(new doFetch()).start();
                break;
            case "send":
                new Thread(new sendMessages()).start();
                break;
            case "download_file":
                xfile_download xfile_load = new xfile_download();
                xfile_load.station = Config.values.stations.get(intent.getIntExtra("nodeindex", 0));
                xfile_load.filename = intent.getStringExtra("filename");

                new Thread(xfile_load).start();
                break;
            case "truncate_echo":
                String echoarea = intent.getStringExtra("echoarea");
                int limit = intent.getIntExtra("limit", 50);

                truncate_echo truncate = new truncate_echo();
                truncate.echoarea = echoarea;
                truncate.limit = limit;

                new Thread(truncate).start();
                break;
            case "blacklist_clear":
                new Thread(new clearBlacklist()).start();
                break;
            case "import_bundle":
                bundleImport import_this = new bundleImport();
                import_this.bundleFile = (File) intent.getSerializableExtra("file");

                new Thread(import_this).start();
                break;
            case "export_bundle":
                bundleExport export_there = new bundleExport();
                if (intent.hasExtra("echoareas")) {
                    export_there.echoareas = intent.getStringArrayListExtra("echoareas");
                } else if (intent.hasExtra("msgids")) {
                    export_there.msgids = intent.getStringArrayListExtra("msgids");
                }
                export_there.where = (File) intent.getSerializableExtra("file");

                new Thread(export_there).start();
                break;
            case "import_blacklist":
                importBlacklist import_bl = new importBlacklist();
                import_bl.where = (File) intent.getSerializableExtra("file");

                new Thread(import_bl).start();
                break;
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
                ExternalStorage.initStorage();
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

            ExternalStorage.initStorage();
            final File new_file = new File(ExternalStorage.rootStorage.getParentFile(), filename);

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

    class clearBlacklist implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int deleted = 0;

            try {
                ExternalStorage.initStorage();
                File blacklist = new File(ExternalStorage.rootStorage, "blacklist.txt");

                if (!blacklist.exists()) {
                    SimpleFunctions.debug("\nФайл чёрного списка пуст!");
                } else {
                    if (!blacklist.canRead()) {
                        SimpleFunctions.debug("Файл чёрного списка недоступен на чтение!");
                    } else {
                        FileInputStream fis = new FileInputStream(blacklist);
                        String[] entries = SimpleFunctions.readIt(fis).split("\n");
                        if (entries.length == 0) SimpleFunctions.debug("ЧС пуст!");
                        else {
                            SimpleFunctions.debug("Начинаем удалять " + entries.length + " сообщений...");
                            for (String entry : entries) {
                                boolean success = GlobalTransport.transport.deleteMessage(entry, null);
                                if (success) {
                                    deleted++;
                                    SimpleFunctions.debug("Deleted " + entry);
                                }
                            }
                        }
                    }
                }

                SimpleFunctions.debug("Удалено сообщений: " + String.valueOf(deleted));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final String finalDeleted = String.valueOf(deleted);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Удалено сообщений: " + finalDeleted, Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }
    }

    class bundleExport implements Runnable {
        public ArrayList<String> echoareas = null;
        public ArrayList<String> msgids = null;
        public File where;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int exported = 0;
            FileOutputStream fos = null;

            try {
                if (where.canWrite()) {
                    if (!where.exists()) {
                        boolean created = where.createNewFile();
                        if (!created) SimpleFunctions.debug("Файл невозможно создать!");
                    }

                    fos = new FileOutputStream(where);
                    if (msgids != null) {
                        SimpleFunctions.debug("Экспорт отдельных сообщений...");
                        SimpleFunctions.debug("Количество: " + String.valueOf(msgids.size()));

                        for (String msgid : msgids) {
                            String rawMessage = GlobalTransport.transport.getRawMessage(msgid);

                            if (rawMessage != null) {
                                String bundleStr = msgid + ":" + Base64.encodeToString(rawMessage.getBytes(), Base64.NO_WRAP) + "\n";
                                fos.write(bundleStr.getBytes());
                                exported++;
                            }
                        }
                    } else if (echoareas != null) {
                        SimpleFunctions.debug("Экспорт по эхоконференциям...");

                        for (String echoarea : echoareas) {
                            ArrayList<String> msglist = GlobalTransport.transport.getMsgList(echoarea, 0, 0);
                            SimpleFunctions.debug(echoarea + ": " + String.valueOf(msglist.size()));

                            for (String msgid : msglist) {
                                String rawMessage = GlobalTransport.transport.getRawMessage(msgid);

                                if (rawMessage != null) {
                                    String bundleStr = msgid + ":" + Base64.encodeToString(rawMessage.getBytes(), Base64.NO_WRAP) + "\n";
                                    fos.write(bundleStr.getBytes());
                                    exported++;
                                }
                            }
                        }
                    }
                }

                SimpleFunctions.debug("Экспортировано: " + String.valueOf(exported));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        SimpleFunctions.debug(e.toString());
                    }
                }
                SimpleFunctions.debugTaskFinished = true;
                final String finalResult = String.valueOf(exported);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Экспортировано: " + finalResult, Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }
    }

    class bundleImport implements Runnable {
        File bundleFile;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int savedMessages = 0;

            try {
                if (bundleFile.exists() && bundleFile.canRead()) {
                    String[] lines = SimpleFunctions.readIt(new FileInputStream(bundleFile)).split("\n");

                    for (String bundle : lines) {
                        String[] pieces = bundle.split(":");

                        if (pieces.length == 2 && !pieces[0].equals("") && !pieces[1].equals("")) {
                            String msgid = pieces[0];
                            String message;

                            try {
                                byte[] rawmsg = Base64.decode(pieces[1], Base64.DEFAULT);
                                message = new String(rawmsg, "UTF-8");
                            } catch (Exception e) {
                                e.printStackTrace();
                                SimpleFunctions.debug("Invalid decoded message: " + pieces[1]);
                                continue;
                            }

                            IIMessage toSave = new IIMessage(message);
                            SimpleFunctions.debug("savemsg " + msgid + " to " + toSave.echo);
                            GlobalTransport.transport.saveMessage(msgid, toSave.echo, toSave);
                            savedMessages++;
                        } else {
                            SimpleFunctions.debug("Wrong message bundle: " + bundle);
                        }
                    }
                } else SimpleFunctions.debug("Файл не существует или недоступен для чтения!");

                SimpleFunctions.debug("Результат: " + String.valueOf(savedMessages));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final String finalResult = String.valueOf(savedMessages);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Результат: " + finalResult, Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }
    }

    class importBlacklist implements Runnable {
        File where;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;

            try {
                ExternalStorage.initStorage();
                if (!where.exists() || !where.canRead()) {
                    SimpleFunctions.debug("Не могу прочитать файл. Может быть, его нет?");
                } else {
                    long oneMB = 8 * 1024 * 1024;
                    if (where.length() > oneMB) {
                        SimpleFunctions.debug("Чёрный список больше одного мегабайта? Ну тебя нафиг!");
                        Thread.sleep(3000);
                    } else {
                        SimpleFunctions.debug("Пробуем скопировать в blacklist.txt рабочего каталога...");
                        FileOutputStream fos = new FileOutputStream(new File(ExternalStorage.rootStorage, "blacklist.txt"));
                        String info = SimpleFunctions.readIt(new FileInputStream(where));
                        fos.write(info.getBytes());
                        fos.close();

                        SimpleFunctions.debug("\nВроде бы, всё прошло нормально. Можно чистить");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                finishTask();
            }
        }
    }

    /*    Предназначено для совершения священного ритуала копипаста
    class sampleTask implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            int result = 0;

            try {
                // делаем все нужные вещи
                SimpleFunctions.debug("Результат: " + String.valueOf(result));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final String finalResult = String.valueOf(result);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Результат: " + finalResult, Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }
    }*/
}