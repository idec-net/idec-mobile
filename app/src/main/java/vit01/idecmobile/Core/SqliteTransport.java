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

public class SqliteTransport extends SQLiteOpenHelper implements AbstractTransport {
    public String tableName = "idecMessages";

    public SqliteTransport(Context context) {
        super(context, "idec-db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + tableName + " ("
                + "number integer primary key autoincrement, "
                + "id text default none,"
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
                + ")");
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
        }
    }

    public ContentValues getContentValues(IIMessage message) {
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

    public IIMessage parseMessage(Cursor cursor) {
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

        SQLiteDatabase db = getWritableDatabase();
        ContentValues readyToInsert = getContentValues(message);

        long result = db.insert(tableName, null, readyToInsert);
        db.close();

        if (result == -1) {
            SimpleFunctions.debug("Cannot save msgid " + msgid);
            return false;
        } else return true;
    }

    public boolean saveMessage(String msgid, String echo, String rawmessage) {
        return saveMessage(msgid, echo, new IIMessage(rawmessage));
    }

    public boolean updateMessage(String msgid, IIMessage message) {
        ContentValues cv = getContentValues(message);

        SQLiteDatabase db = getWritableDatabase();
        int result = db.update(tableName, cv, "id = ?", new String[]{msgid});
        db.close();

        return result > 0;
    }

    public boolean updateMessage(String msgid, String rawmessage) {
        return updateMessage(msgid, new IIMessage(rawmessage));
    }

    public boolean deleteMessage(String msgid, String echo) {
        SQLiteDatabase db = getWritableDatabase();

        int result = db.delete(tableName, "id = ? and echoarea = ?", new String[]{msgid, echo});
        db.close();

        return result > 0;
    }

    public void deleteMessages(ArrayList<String> msgids, String echo) {
        SQLiteDatabase db = getWritableDatabase();

        for (String msgid : msgids) {
            db.delete(tableName, "id = ? and echoarea = ?", new String[]{msgid, echo});
        }
        db.close();
    }

    public ArrayList<String> fetch_rows(Cursor cursor) {
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

    public ArrayList<String> getMsgList(String echo, int offset, int length) {
        SQLiteDatabase db = getReadableDatabase();

        String limitstr;
        if (offset > 0 && length > 0) {
            limitstr = String.valueOf(offset) + ", " + String.valueOf(length);
        } else limitstr = null;

        Cursor cursor = db.query(tableName, new String[]{"id", "number"}, "echoarea = ?",
                new String[]{echo}, null, null, "number", limitstr);

        ArrayList<String> result = fetch_rows(cursor);
        db.close();
        return result;
    }

    public void deleteEchoarea(String echo, boolean with_contents) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(tableName, "echoarea = ?", new String[]{echo});
        db.close();
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
        db.close();
        return result;
    }

    public ArrayList<String> fullEchoList() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(true, tableName, new String[]{"echoarea"},
                null, null, null, null, null, null);

        ArrayList<String> results = fetch_rows(cursor);

        db.close();
        return results;
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
        db.close();
        return result;
    }

    public int countUnread(String echo) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, new String[]{"count(*)"},
                "echoarea = ? and isunread=1", new String[]{echo}, null, null, null);

        int result = 0;

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return result;
    }

    public void FuckDeleteEverything() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(tableName, null, null);
        db.close();
    }

    public ArrayList<String> msgidsBySelection(String selection_block, String sort, String limit) {
        SQLiteDatabase db = getReadableDatabase();

        if (sort != null) sort = " " + sort;
        else sort = "";

        Cursor cursor = db.query(true, tableName, new String[]{"id, number"},
                selection_block, null, null, null, "number" + sort, limit);

        ArrayList<String> result = fetch_rows(cursor);
        db.close();
        return result;
    }

    public ArrayList<String> getFavorites() {
        return msgidsBySelection("isfavorite=1", null, null);
    }

    public ArrayList<String> getUnreadMessages(String echoarea) {
        String selection_block = (echoarea != null) ? "echoarea='" + echoarea + "'" : null;
        selection_block += " and isunread=1";

        return msgidsBySelection(selection_block, null, null);
    }

    public ArrayList<String> getAllUnreadMessages() {
        return msgidsBySelection("isunread=1", null, null);
    }

    @Override
    public echoStat getUnreadStats(String echo) {
        echoStat result = new echoStat();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, new String[]{"count(*)"},
                "echoarea = ? and isunread=1", new String[]{echo}, null, null, null);

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result.unread_count = cursor.getInt(0);
        }

        cursor.close();

        cursor = db.query(tableName, new String[]{"count(*)"},
                "echoarea = ?", new String[]{echo}, null, null, null);

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            result.total_count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return result;
    }

    public ArrayList<String> getUnreadEchoareas() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(true, tableName, new String[]{"echoarea"}, "isunread=1", null, null, null, null, null);

        ArrayList<String> result = fetch_rows(cursor);
        db.close();
        return result;
    }

    public ArrayList<String> getUnreadFavorites() {
        String selection_block = "isfavorite=1 and isunread=1";
        return msgidsBySelection(selection_block, null, null);
    }

    public ArrayList<String> messagesToUsers(List<String> users_to, int limit, boolean unread) {
        if (users_to.size() == 0) return new ArrayList<>();

        String selection_block;

        if (users_to.size() == 1) {
            selection_block = "msgto='" + users_to.get(0) + "'";
        } else {
            selection_block = "msgto='" + TextUtils.join("' or msgto='", users_to) + "'";
        }

        if (unread) {
            selection_block += " and isunread=1";
        }

        // получаем последние limit сообщений к нужным юзерам в порядке по возрастанию id
        ArrayList<String> selected = msgidsBySelection(selection_block, "desc", "0," + String.valueOf(limit));
        Collections.reverse(selected);
        return selected;
    }

    public void updateBooleanField(String field, boolean value, List<String> msgids) {
        if (msgids.size() == 0) SimpleFunctions.debug(field + " update failed: empty input!");

        SQLiteDatabase db = getWritableDatabase();
        int value_to_insert = (value) ? 1 : 0;
        String clause_part;

        if (msgids.size() == 1) {
            clause_part = "id='" + msgids.get(0) + "'";
        } else {
            clause_part = "id='" + TextUtils.join("' or id='", msgids) + "'";
        }

        db.execSQL("update " + tableName + " set " + field + "="
                + String.valueOf(value_to_insert) + " where " + clause_part);
        db.close();
    }

    public void setUnread(boolean unread, List<String> msgids) {
        updateBooleanField("isunread", unread, msgids);
    }

    public void setUnread(boolean unread, String area) {
        SQLiteDatabase db = getWritableDatabase();
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
        db.close();
    }

    public void setFavorite(boolean favorite, List<String> msgids) {
        updateBooleanField("isfavorite", favorite, msgids);
    }
}