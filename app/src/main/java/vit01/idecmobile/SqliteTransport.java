package vit01.idecmobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Hashtable;

public class SqliteTransport extends SQLiteOpenHelper implements AbstractTransport {
    public String tableName = "idecMessages";

    SqliteTransport(Context context) {
        super(context, "idec-db", null, 1);
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
                + "msg text not null )");
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

    public ArrayList<String> getMsgList(String echo, int offset, int length) {
        ArrayList<String> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String limitstr;
        if (offset > 0 && length > 0) {
            limitstr = String.valueOf(offset) + ", " + String.valueOf(length);
        } else limitstr = null;

        Cursor cursor = db.query(tableName, new String[]{"id", "number"}, "echoarea = ?",
                new String[]{echo}, null, null, "date", limitstr);

        // TODO: исправить в фетчере сохранение, потому что не работает сортировка по числу

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        }

        cursor.close();
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


        String args = "id='" + TextUtils.join("' or id='", msgids.toArray()) + "'";
        Hashtable<String, IIMessage> result = new Hashtable<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(tableName, null, args, null, null, null, null);

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                IIMessage message = parseMessage(cursor);
                String msgid = message.id;

                result.put(msgid, message);
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

        ArrayList<String> results = new ArrayList<>();

        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                results.add(cursor.getString(cursor.getColumnIndex("echoarea")));
            }
        }

        cursor.close();
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

    void FuckDeleteEverything() {
        SQLiteDatabase db = getReadableDatabase();
        db.delete(tableName, null, null);
        db.close();
    }
}
