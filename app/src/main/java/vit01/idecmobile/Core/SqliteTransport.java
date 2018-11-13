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

package vit01.idecmobile.Core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import vit01.idecmobile.prefs.Config;

class SqliteTransport extends SQLiteOpenHelper implements AbstractTransport {
    private static SQLiteDatabase db_static = null;
    private Context savedcontext;

    private String tableName = "idecMessages";
    private String echoIndexName = "echostats";

    private String filesTableName = "idecFiles";
    private String fechoIndexName = "fechostats";

    private String messagesDbCreate = "create table " + tableName + " ("
            + "number integer primary key autoincrement,"
            + "id text default null,"
            + "tags text,"
            + "echoarea text not null,"
            + "date bigint default 0,"
            + "msgfrom text,"
            + "addr text,"
            + "msgto text,"
            + "subj text not null,"
            + "msg text not null,"
            + "isfavorite integer default 0,"
            + "isunread integer default 1"
            + ")";

    private String filesDbCreate = "create table " + filesTableName + " ("
            + "number integer primary key autoincrement,"
            + "id text default null,"
            + "tags text,"
            + "fecho text not null,"
            + "filename text not null,"
            + "serversize bigint default 0,"
            + "addr text,"
            + "description text default null"
            + ")";

    private String messagesIndexCreate = "create index if not exists " +
            echoIndexName + " on " + tableName + " (echoarea, isunread)";


    private String filesIndexCreate = "create index if not exists " +
            fechoIndexName + " on " + filesTableName + " (fecho)";


    SqliteTransport(Context context) {
        super(context, "idec-db", null, 4);
        savedcontext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(messagesDbCreate);
        db.execSQL(messagesIndexCreate);
        db.execSQL(filesDbCreate);
        db.execSQL(filesIndexCreate);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("alter table " + tableName + " add isfavorite integer default 0");
            db.execSQL("alter table " + tableName + " add isunread integer default 1");

            oldVersion++;
            newVersion++;
        }

        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL(messagesIndexCreate);
            oldVersion++;
            newVersion++;
        }

        if (oldVersion == 3 && newVersion == 4) {
            db.execSQL(filesDbCreate);
            db.execSQL(filesIndexCreate);

            boolean configUpdate = false;
            for (Station station : Config.values.stations) {
                if (station.file_echoareas == null) station.file_echoareas = new ArrayList<>();
                if (station.file_echoareas.size() == 0) {
                    configUpdate = true;
                    station.file_echoareas.add("books.tech");
                    station.file_echoareas.add("pictures");
                    station.file_echoareas.add("mlp.pictures");
                }
            }

            if (configUpdate) Config.writeConfig(savedcontext);
        }
    }

    private SQLiteDatabase getDb() {
        if (db_static == null) db_static = getWritableDatabase();
        while (db_static.isDbLockedByCurrentThread()) {
            try {
                SimpleFunctions.debug("Locked");
                wait(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
                SimpleFunctions.debug("db locked by current thread");
            }
        }

        return db_static;
    }

    private ContentValues getContentValues(IIMessage message) {
        ContentValues cv = new ContentValues();
        cv.put("id", message.id);
        cv.put("echoarea", message.echo);
        cv.put("tags", message.getTags());
        cv.put("date", message.time);
        cv.put("msgfrom", message.from);
        cv.put("addr", message.addr);
        cv.put("msgto", message.to);
        cv.put("subj", message.subj);
        cv.put("msg", message.msg);
        cv.put("isfavorite", (message.is_favorite) ? 1 : 0);
        cv.put("isunread", (message.is_unread) ? 1 : 0);

        return cv;
    }

    private IIMessage parseMessage(Cursor cursor) {
        IIMessage msg = new IIMessage();

        msg.id = cursor.getString(cursor.getColumnIndex("id"));
        msg.echo = cursor.getString(cursor.getColumnIndex("echoarea"));
        msg.tags = IIMessage.parseTags(cursor.getString(cursor.getColumnIndex("tags")));
        msg.time = cursor.getLong(cursor.getColumnIndex("date"));
        msg.from = cursor.getString(cursor.getColumnIndex("msgfrom"));
        msg.addr = cursor.getString(cursor.getColumnIndex("addr"));
        msg.to = cursor.getString(cursor.getColumnIndex("msgto"));
        msg.subj = cursor.getString(cursor.getColumnIndex("subj"));
        msg.msg = cursor.getString(cursor.getColumnIndex("msg"));
        msg.repto = (msg.tags.containsKey("repto")) ? msg.tags.get("repto") : null;
        msg.is_favorite = (cursor.getInt(cursor.getColumnIndex("isfavorite")) == 1);
        msg.is_unread = (cursor.getInt(cursor.getColumnIndex("isunread")) == 1);

        return msg;
    }

    public boolean saveMessage(String msgid, String echo, IIMessage message) {
        message.id = msgid;

        SQLiteDatabase db = getDb();
        ContentValues readyToInsert = getContentValues(message);

        long result = db.insert(tableName, null, readyToInsert);

        if (result == -1) {
            SimpleFunctions.debug("Cannot save msgid " + msgid);
            return false;
        } else return true;
    }

    public boolean saveMessage(String msgid, String echo, String rawmessage) {
        return saveMessage(msgid, echo, new IIMessage(rawmessage));
    }

    public boolean updateMessage(String msgid, IIMessage message) {
        if (msgid == null) {
            return false;
        }
        message.id = msgid;
        ContentValues cv = getContentValues(message);

        SQLiteDatabase db = getDb();
        int result = db.update(tableName, cv, "id = ?", new String[]{msgid});

        return result > 0;
    }

    public boolean updateMessage(String msgid, String rawmessage) {
        return updateMessage(msgid, new IIMessage(rawmessage));
    }

    public boolean deleteMessage(String msgid, String echo) {
        SQLiteDatabase db = getDb();

        String whereClause = "id = ?";
        String[] whereArgs;

        if (echo != null) {
            whereClause += " and echoarea = ?";
            whereArgs = new String[]{msgid, echo};
        } else whereArgs = new String[]{msgid};

        int result = db.delete(tableName, whereClause, whereArgs);

        return result > 0;
    }

    public void deleteMessages(ArrayList<String> msgids, String echo) {
        SQLiteDatabase db = getDb();

        for (String msgid : msgids) {
            db.delete(tableName, "id = ? and echoarea = ?", new String[]{msgid, echo});
        }
    }

    private ArrayList<String> fetch_rows(Cursor cursor) {
        ArrayList<String> result = new ArrayList<>();

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            boolean needToClose = false;

            while (!needToClose) {
                result.add(cursor.getString(0));

                if (!cursor.moveToNext()) needToClose = true;
            }
        }

        cursor.close();
        return result;
    }

    public ArrayList<String> getMsgList(String echo, int offset, int length, String sort) {
        SQLiteDatabase db = getReadableDatabase();

        String limitstr;
        if (offset >= 0 && length > 0) {
            limitstr = String.valueOf(offset) + ", " + String.valueOf(length);
        } else limitstr = null;

        Cursor cursor = db.query(tableName, new String[]{"id", "number"}, "echoarea = ?",
                new String[]{echo}, null, null, sort, limitstr);

        return fetch_rows(cursor);
    }

    public void deleteEchoarea(String echo, boolean with_contents) {
        SQLiteDatabase db = getDb();
        db.delete(tableName, "echoarea = ?", new String[]{echo});
    }

    public String getRawMessage(String msgid) {
        return getMessage(msgid).raw();
    }

    public Hashtable<String, String> getRawMessages(ArrayList<String> msgids) {
        Hashtable<String, String> result = new Hashtable<>();
        Hashtable<String, IIMessage> normalMessages = getMessages(msgids);

        for (String msgid : msgids) {
            result.put(msgid, normalMessages.get(msgid).raw());
        }
        return result;
    }

    public IIMessage getMessage(String msgid) {
        ArrayList<String> msgidArr = new ArrayList<>();
        msgidArr.add(msgid);

        Hashtable<String, IIMessage> results = getMessages(msgidArr);
        return results.get(msgid);
    }

    public Hashtable<String, IIMessage> getMessages(ArrayList<String> msgids) {
        // Будем считать, что слепить запрос с кучей OR - это норма
        // В конце концов, в ii-php всё точно так же

        String args;
        if (msgids.size() == 1) {
            args = "id='" + msgids.get(0) + "'";
        } else {
            args = "id='" + TextUtils.join("' or id='", msgids) + "'";
        }

        Hashtable<String, IIMessage> result = new Hashtable<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, null, args, null, null, null, null);

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            boolean needToClose = false;

            while (!needToClose) {
                IIMessage message = parseMessage(cursor);
                String msgid = message.id;

                result.put(msgid, message);
                if (!cursor.moveToNext()) needToClose = true;
            }
        }

        cursor.close();
        return result;
    }

    public ArrayList<String> fullEchoList() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(true, tableName, new String[]{"echoarea"},
                null, null, null, null, null, null);

        return fetch_rows(cursor);
    }

    public int countMessages(String echo) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, new String[]{"count(*)"},
                "echoarea = ?", new String[]{echo},
                null, null, null);

        int result = 0;

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        return result;
    }

    public int countUnread() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, new String[]{"count(*)"},
                "isunread=1", null, null, null, null);

        int result = 0;

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        return result;
    }

    @Override
    public int countFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, new String[]{"count(*)"},
                "isfavorite=1", null, null, null, null);

        int result = 0;

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        return result;
    }

    public void FuckDeleteEverything() {
        SQLiteDatabase db = getDb();
        db.delete(tableName, null, null);
        db.delete(filesTableName, null, null);
    }

    private ArrayList<String> msgidsBySelection(String selection_block, String sort, String limit) {
        SQLiteDatabase db = getReadableDatabase();

        if (sort == null) sort = "";

        Cursor cursor = db.query(true, tableName, new String[]{"id, number"},
                selection_block, null, null, null, sort, limit);

        return fetch_rows(cursor);
    }

    public ArrayList<String> getFavorites(String sort) {
        if (sort == null) sort = "date";
        return msgidsBySelection("isfavorite=1", sort, null);
    }

    public ArrayList<String> getUnreadMessages(String echoarea, String sort) {
        if (sort == null) sort = "date";

        String selection_block = (echoarea != null) ? "echoarea='" + echoarea + "'" : null;
        selection_block += " and isunread=1";

        return msgidsBySelection(selection_block, sort, null);
    }

    public ArrayList<String> getAllUnreadMessages(String sort) {
        if (sort == null) sort = "date";
        return msgidsBySelection("isunread=1", sort, null);
    }

    @Override
    public void getUnreadStats(ArrayList<String> echoareas, ArrayList<echoStat> result) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;
        int unread_count, total_count;

        for (String echo : echoareas) {
            unread_count = 0;
            total_count = 0;

            cursor = db.query(tableName, new String[]{"count(*)"},
                    "echoarea = ? and isunread=1", new String[]{echo}, null, null, null);

            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                unread_count = cursor.getInt(0);
            }

            cursor.close();

            cursor = db.query(tableName, new String[]{"count(*)"},
                    "echoarea = ?", new String[]{echo}, null, null, null);

            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                total_count = cursor.getInt(0);
            }

            cursor.close();
            result.add(new echoStat(total_count, unread_count));
        }
    }

    public ArrayList<String> getUnreadFavorites(String sort) {
        if (sort == null) sort = "date";

        String selection_block = "isfavorite=1 and isunread=1";
        return msgidsBySelection(selection_block, sort, null);
    }

    public ArrayList<String> messagesToUsers(List<String> users_to, int limit, boolean unread, String sort) {
        if (users_to.size() == 0) return new ArrayList<>();

        String selection_block;

        if (users_to.size() == 1) {
            selection_block = "msgto like '%" + users_to.get(0) + "%'";
        } else {
            selection_block = "msgto like '%" + TextUtils.join("%' or msgto like '%", users_to) + "%'";
        }

        if (unread) {
            selection_block += " and isunread=1";
        }

        // получаем последние limit сообщений к нужным юзерам в порядке по возрастанию id
        ArrayList<String> selected = msgidsBySelection(selection_block, sort + " desc", "0," + String.valueOf(limit));
        Collections.reverse(selected);
        return selected;
    }

    private void updateBooleanField(String field, boolean value, List<String> msgids) {
        if (msgids.size() == 0) SimpleFunctions.debug(field + " update failed: empty input!");

        SQLiteDatabase db = getDb();
        int value_to_insert = (value) ? 1 : 0;
        String clause_part;

        if (msgids.size() == 1) {
            clause_part = "id='" + msgids.get(0) + "'";
        } else {
            clause_part = "id='" + TextUtils.join("' or id='", msgids) + "'";
        }

        db.execSQL("update " + tableName + " set " + field + "="
                + String.valueOf(value_to_insert) + " where " + clause_part);
    }

    public void setUnread(boolean unread, List<String> msgids) {
        updateBooleanField("isunread", unread, msgids);
    }

    public void setUnread(boolean unread, String area) {
        SQLiteDatabase db = getDb();
        int value_to_insert = (unread) ? 1 : 0;
        String clause_part;

        if (area == null) {
            clause_part = "1";
        } else if (area.equals("_favorites")) {
            clause_part = "isfavorite=1";
        } else {
            clause_part = "echoarea='" + area + "'";
        }

        db.execSQL("update " + tableName + " set isunread="
                + String.valueOf(value_to_insert) + " where " + clause_part);
    }

    public void setFavorite(boolean favorite, List<String> msgids) {
        updateBooleanField("isfavorite", favorite, msgids);
    }

    public ArrayList<String> searchQuery(
            String messageKey, String subjKey,
            List<String> echoareas, List<String> senders, List<String> receivers, List<String> addresses,
            Long time1, Long time2, boolean is_favorite) {

        ArrayList<String> selectionKeys = new ArrayList<>();

        if (echoareas != null && echoareas.size() > 0)
            selectionKeys.add("echoarea='" + TextUtils.join("' or echoarea='", echoareas) + "'");

        if (senders != null && senders.size() > 0)
            selectionKeys.add("msgfrom like '%" + TextUtils.join("%' or msgfrom like '%", senders) + "%'");

        if (receivers != null && receivers.size() > 0)
            selectionKeys.add("msgto like '%" + TextUtils.join("%' or msgto like '%", receivers) + "%'");

        if (addresses != null && addresses.size() > 0)
            selectionKeys.add("addr like '%" + TextUtils.join("' or addr like '%", addresses) + "%'");

        if (subjKey != null)
            selectionKeys.add("subj like '%" + subjKey + "%'");

        if (messageKey != null)
            selectionKeys.add("msg like '%" + messageKey + "%' or id='" + messageKey + "'");

        if (time1 != null && time2 != null)
            selectionKeys.add("date>=" + String.valueOf(time1) + " and date<=" + String.valueOf(time2));

        if (is_favorite)
            selectionKeys.add("isfavorite=1");

        String clause_part = "(" + TextUtils.join(") and (", selectionKeys) + ")";

        if (clause_part.equals("()")) {
            SimpleFunctions.debug("Error: empty query");
            return new ArrayList<>();
        } else SimpleFunctions.debug(clause_part);

        return msgidsBySelection(clause_part, "number", null);
    }

    private ContentValues filesGetContentValues(FEchoFile entry) {
        ContentValues cv = new ContentValues();
        cv.put("id", entry.id);
        cv.put("tags", IIMessage.collectTags(entry.tags));
        cv.put("fecho", entry.fecho);
        cv.put("filename", entry.filename);
        cv.put("serversize", entry.serverSize);
        cv.put("addr", entry.addr);
        cv.put("description", entry.description);

        return cv;
    }

    private FEchoFile parseFileEntry(Cursor cursor) {
        FEchoFile entry = new FEchoFile();

        entry.id = cursor.getString(cursor.getColumnIndex("id"));
        entry.tags = IIMessage.parseTags(cursor.getString(cursor.getColumnIndex("tags")));
        entry.fecho = cursor.getString(cursor.getColumnIndex("fecho"));
        entry.filename = cursor.getString(cursor.getColumnIndex("filename"));
        entry.serverSize = cursor.getLong(cursor.getColumnIndex("serversize"));
        entry.addr = cursor.getString(cursor.getColumnIndex("addr"));
        entry.description = cursor.getString(cursor.getColumnIndex("description"));

        return entry;
    }

    public boolean saveFileMeta(String fid, String fecho, FEchoFile entry) {
        entry.id = fid;
        if (entry.tags.size() == 0) {
            entry.tags.put("ii", "ok");
        }
        entry.fecho = fecho;

        SQLiteDatabase db = getDb();
        ContentValues readyToInsert = filesGetContentValues(entry);

        long result = db.insert(filesTableName, null, readyToInsert);

        if (result == -1) {
            SimpleFunctions.debug("Cannot save fid " + fid);
            return false;
        } else return true;
    }

    public boolean saveFileMeta(String fid, String echo, String rawentry) {
        return saveFileMeta(fid, echo, new FEchoFile(rawentry));
    }

    public boolean updateFileMeta(String fid, FEchoFile entry) {
        if (fid == null) {
            return false;
        }
        entry.id = fid;
        if (entry.tags.size() == 0) {
            entry.tags.put("ii", "ok");
        }

        ContentValues cv = filesGetContentValues(entry);

        SQLiteDatabase db = getDb();
        int result = db.update(filesTableName, cv, "id = ?", new String[]{fid});

        return result > 0;
    }

    public boolean updateFileMeta(String fid, String echo, String rawentry) {
        return saveFileMeta(fid, echo, new FEchoFile(rawentry));
    }


    public boolean deleteFileEntry(String fid, String fecho) {
        SQLiteDatabase db = getDb();

        String whereClause = "id = ?";
        String[] whereArgs;

        if (fecho != null) {
            whereClause += " and fecho = ?";
            whereArgs = new String[]{fid, fecho};
        } else whereArgs = new String[]{fid};

        int result = db.delete(filesTableName, whereClause, whereArgs);

        return result > 0;
    }

    public void deleteFileEntries(ArrayList<String> fids, String fecho) {
        SQLiteDatabase db = getDb();

        for (String fid : fids) {
            db.delete(filesTableName, "id = ? and fecho = ?", new String[]{fid, fecho});
        }
    }

    public ArrayList<String> getFileList(String fecho, int offset, int length, String sort) {
        SQLiteDatabase db = getReadableDatabase();

        String limitstr;
        if (offset >= 0 && length > 0) {
            limitstr = String.valueOf(offset) + ", " + String.valueOf(length);
        } else limitstr = null;

        Cursor cursor = db.query(true, filesTableName, new String[]{"id", "number"}, "fecho = ?",
                new String[]{fecho}, null, null, sort, limitstr);

        return fetch_rows(cursor);
    }

    public void deleteFileEchoarea(String fecho, boolean with_contents) {
        SQLiteDatabase db = getDb();
        db.delete(filesTableName, "fecho = ?", new String[]{fecho});
    }

    public FEchoFile getFileMeta(String fid) {
        ArrayList<String> msgidArr = new ArrayList<>();
        msgidArr.add(fid);

        Hashtable<String, FEchoFile> results = getFilesMeta(msgidArr);
        return results.get(fid);
    }

    public Hashtable<String, FEchoFile> getFilesMeta(ArrayList<String> msgids) {
        String args;
        if (msgids.size() == 1) {
            args = "id='" + msgids.get(0) + "'";
        } else {
            args = "id='" + TextUtils.join("' or id='", msgids) + "'";
        }

        Hashtable<String, FEchoFile> result = new Hashtable<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(filesTableName, null, args, null, null, null, null);

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            boolean needToClose = false;

            while (!needToClose) {
                FEchoFile entry = parseFileEntry(cursor);
                String fid = entry.id;

                result.put(fid, entry);
                if (!cursor.moveToNext()) needToClose = true;
            }
        }

        cursor.close();
        return result;
    }

    public ArrayList<String> fullFEchoList() {
        SQLiteDatabase db = getDb();

        Cursor cursor = db.query(true, filesTableName, new String[]{"fecho"},
                null, null, null, null, null, null);

        return fetch_rows(cursor);
    }

    public int countFiles(String fecho) {
        SQLiteDatabase db = getDb();
        Cursor cursor = db.query(filesTableName, new String[]{"count(*)"},
                "fecho = ?", new String[]{fecho},
                null, null, null);

        int result = 0;

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        return result;
    }


    private ArrayList<String> fidsBySelection(String selection_block, String sort, String limit) {
        SQLiteDatabase db = getDb();

        if (sort == null) sort = "";

        Cursor cursor = db.query(true, filesTableName, new String[]{"id, number"},
                selection_block, null, null, null, sort, limit);

        return fetch_rows(cursor);
    }

    public ArrayList<String> fileSearchQuery(List<String> fechoes, List<String> filenames,
                                             List<String> addresses, String descriptionKey) {

        ArrayList<String> selectionKeys = new ArrayList<>();

        if (fechoes != null && fechoes.size() > 0)
            selectionKeys.add("fecho='" + TextUtils.join("' or fecho='", fechoes) + "'");

        if (filenames != null && filenames.size() > 0)
            selectionKeys.add("filename like '%" + TextUtils.join("%' or filename like '%", filenames) + "%'");

        if (addresses != null && addresses.size() > 0)
            selectionKeys.add("addr like '%" + TextUtils.join("' or addr like '%", addresses) + "%'");

        if (descriptionKey != null)
            selectionKeys.add("description like '%" + descriptionKey + "%'");

        String clause_part = "(" + TextUtils.join(") and (", selectionKeys) + ")";

        if (clause_part.equals("()")) {
            SimpleFunctions.debug("Error: empty query");
            return new ArrayList<>();
        } else SimpleFunctions.debug(clause_part);

        return fidsBySelection(clause_part, "number", null);
    }

    public ArrayList<String> searchSimilarMsgids(String msgidKey) {
        // used for lowercase msgid resolving
        return msgidsBySelection("id like '%" + msgidKey + "%'", "number", null);
    }

    // TODO: get fechoarea full contents
    // TODO: files search (filter)
}