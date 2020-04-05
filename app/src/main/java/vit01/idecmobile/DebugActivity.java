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

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Blacklist;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.prefs.Config;

public class DebugActivity extends AppCompatActivity {
    ScrollView debugLayout;
    TextView textView;
    View decorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setActivityTitle(this, getString(R.string.title_activity_debug));

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        debugLayout = findViewById(R.id.debugLayout);
        textView = findViewById(R.id.debug_view);
        textView.setText("");

        SimpleFunctions.debugTaskFinished = false;

        new Thread(new updateDebug()).start();

        Intent intent = getIntent();
        String task = intent.getStringExtra("task");

        switch (task) {
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
        Toast.makeText(DebugActivity.this, R.string.close_window_debug, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onStop() {
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        super.onStop();
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

    class xfile_download implements Runnable {
        public Station station;
        public String filename;

        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            boolean success = false;

            final File new_file = new File(ExternalStorage.rootStorage.getParentFile(), filename);

            try {
                success = Fetcher.xfile_download(getApplicationContext(), station, filename, new_file);
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
            } finally {
                SimpleFunctions.debugTaskFinished = true;

                final boolean finalSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String result = (finalSuccess)
                                ? getString(R.string.file_loaded_to, new_file.getAbsolutePath())
                                : getString(R.string.done_with_errors);
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
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

                SimpleFunctions.debug(getString(R.string.truncate_limit_display, limit, countMessages));
                if (countMessages <= limit) needTruncate = false;
                else {
                    int deleteLength = countMessages - limit;
                    SimpleFunctions.debug(getString(R.string.inserting_to_list));
                    ArrayList<String> deleteThem = transport.getMsgList(echoarea, 0, deleteLength, "number");
                    SimpleFunctions.debug(getString(R.string.deleting_useless_messages));
                    transport.deleteMessages(deleteThem, echoarea);
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
            } finally {
                SimpleFunctions.debugTaskFinished = true;

                final boolean finalNeedTruncate = needTruncate;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int answer = (finalNeedTruncate) ? R.string.echoarea_clear_done : R.string.echoarea_nothing_to_truncate;
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
                Blacklist.loadBlacklist();

                if (Blacklist.badMsgids.size() == 0)
                    SimpleFunctions.debug(getString(R.string.blacklist_empty));
                else {
                    SimpleFunctions.debug(getString(R.string.deleting_n_messages, Blacklist.badMsgids.size()));
                    for (String entry : Blacklist.badMsgids) {
                        boolean success = GlobalTransport.transport.deleteMessage(entry, null);
                        if (success) {
                            deleted++;
                            SimpleFunctions.debug("Deleted " + entry);
                        }
                    }
                }

                SimpleFunctions.debug(getString(R.string.deleted_n_messages, deleted));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final int finalDeleted = deleted;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.deleted_n_messages, finalDeleted), Toast.LENGTH_SHORT).show();
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
                if (!where.exists()) {
                    boolean created = where.createNewFile();
                    if (!created)
                        SimpleFunctions.debug(getString(R.string.create_file_error) + " " + where.getAbsolutePath());
                }
                if (where.canWrite()) {
                    fos = new FileOutputStream(where);

                    if (msgids == null) msgids = new ArrayList<>();
                    if (msgids.size() > 0) {
                        SimpleFunctions.debug(getString(R.string.exporting_certain_messages));
                        SimpleFunctions.debug(getString(R.string.quantity, msgids.size()));
                        exported += exportThese(fos, msgids);
                    } else if (echoareas != null) {
                        SimpleFunctions.debug(getString(R.string.exporting_by_echoes));

                        for (String echoarea : echoareas) {
                            msgids.addAll(GlobalTransport.transport.getMsgList(echoarea, 0, 0, "number"));
                            exported += exportThese(fos, msgids);

                            SimpleFunctions.debug(echoarea + ": " + msgids.size());
                            msgids.clear();
                        }
                    }
                    SimpleFunctions.debug(getString(R.string.done));

                } else
                    SimpleFunctions.debug(where.getAbsolutePath() + getString(R.string.unable_to_write_error));

                SimpleFunctions.debug(getString(R.string.exported_n_in_file, exported, where.getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
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
                final int finalResult = exported;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.exported_n_messages, finalResult), Toast.LENGTH_SHORT).show();
                    }
                });

                finishTask();
            }
        }

        public int exportThese(FileOutputStream fos, ArrayList<String> msgids) throws IOException {
            int exported = 0;
            for (String msgid : msgids) {
                IIMessage message = GlobalTransport.transport.getMessage(msgid);

                if (message != null && message.id != null) {
                    // Это нужно, чтобы сисоп станции не мог навязать поинту добавление сообщения в избранное
                    message.tags.remove("idecmobile-favorite");
                    if (message.is_favorite) message.tags.put("idecmobile-favorite", "true");
                    String bundleStr = msgid + ":" + Base64.encodeToString(message.raw().getBytes(), Base64.NO_WRAP) + "\n";
                    fos.write(bundleStr.getBytes());
                    exported++;
                }
            }
            return exported;
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
                                message = new String(rawmsg, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                e.printStackTrace();
                                SimpleFunctions.debug("Invalid decoded message: " + pieces[1]);
                                continue;
                            }

                            IIMessage toSave = new IIMessage(message);
                            SimpleFunctions.debug("savemsg " + msgid + " to " + toSave.echo);

                            String favorite = toSave.tags.get("idecmobile-favorite");
                            if (favorite != null && favorite.equals("true")) {
                                toSave.is_favorite = true;
                                toSave.tags.remove("idecmobile-favorite");
                            }
                            GlobalTransport.transport.saveMessage(msgid, toSave.echo, toSave);
                            savedMessages++;
                        } else {
                            SimpleFunctions.debug("Wrong message bundle: " + bundle);
                        }
                    }
                } else SimpleFunctions.debug(getString(R.string.no_file_warning));

                SimpleFunctions.debug(getString(R.string.imported_n_messages, savedMessages));
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                final int finalResult = savedMessages;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.messages_quantity, finalResult), Toast.LENGTH_SHORT).show();
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
                if (!where.exists() || !where.canRead()) {
                    SimpleFunctions.debug(getString(R.string.no_file_warning));
                } else {
                    long oneMB = 1024 * 1024;
                    if (where.length() > oneMB) {
                        SimpleFunctions.debug(getString(R.string.blacklist_too_big));
                        Thread.sleep(3000);
                    } else {
                        SimpleFunctions.debug(getString(R.string.trying_to_copy_something, Blacklist.filename));
                        FileOutputStream fos = new FileOutputStream(new File(ExternalStorage.rootStorage, Blacklist.filename));
                        String info = SimpleFunctions.readIt(new FileInputStream(where));
                        fos.write(info.getBytes());
                        fos.close();

                        SimpleFunctions.debug(getString(R.string.blacklist_ram_loading));
                        Blacklist.loadBlacklist();
                        SimpleFunctions.debug("\n" + getString(R.string.blacklist_clear_ready));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug(getString(R.string.error_formatted, e.toString()));
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
