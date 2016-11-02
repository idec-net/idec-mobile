package vit01.idecmobile.Core;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class ExternalStorage {
    public static File rootStorage;
    public static boolean filtersPresent = false;
    static String dataDirectory = "idecMobile";
    static FilenameFilter draftsFilter;
    static FilenameFilter sentFilter;

    ExternalStorage() {
    }

    public static void initStorage() {
        if (!isExternalStorageWritable()) {
            SimpleFunctions.debug("External storage is not writable!");
        }

        rootStorage = new File(Environment.getExternalStorageDirectory(), dataDirectory);

        if (!rootStorage.exists() && !rootStorage.mkdirs()) {
            SimpleFunctions.debug("Root directory for drafts not created!");
        }

        if (filtersPresent) return;

        draftsFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".toss");
            }
        };

        sentFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".out");
            }
        };

        filtersPresent = true;
    }

    public static File getStationStorageDir(String outbox_id) {
        File file = new File(rootStorage, outbox_id);
        if (!file.exists() && !file.mkdirs()) {
            SimpleFunctions.debug("Directory not created");
            return null;
        }
        return file;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static ArrayList<File> getDraftsInside(File directory, boolean unsent) {
        FilenameFilter filter = (unsent) ? draftsFilter : sentFilter;

        File[] contents = directory.listFiles(filter);
        ArrayList<File> result = new ArrayList<>();

        if (contents != null) {
            Collections.addAll(result, contents);
        }

        return result;
    }

    public static ArrayList<File> getAllDrafts(boolean unsent) {
        ArrayList<File> result = new ArrayList<>();

        for (Station station : Config.values.stations) {
            File stationDir = getStationStorageDir(station.outbox_storage_id);
            if (stationDir == null) continue;
            ArrayList<File> current = getDraftsInside(stationDir, unsent);
            result.addAll(current);
        }

        return result;
    }

    public static DraftMessage readDraft(File file) {
        String contents;
        try {
            FileInputStream fis = new FileInputStream(file);
            contents = SimpleFunctions.readIt(fis);
            fis.close();
        } catch (Exception e) {
            SimpleFunctions.debug(e.toString());
            e.printStackTrace();
            return null;
        }

        return new DraftMessage(contents);
    }

    public static boolean writeDraftToFile(File file, DraftMessage message) {
        String contents = message.raw();

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(contents.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            SimpleFunctions.debug(e.toString());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static int getAndIncrementNumber(String outbox_id) {
        File directory = getStationStorageDir(outbox_id);
        if (directory == null) return -1;

        String[] inside = directory.list();
        ArrayList<String> contents = new ArrayList<>();

        if (inside != null) {
            Collections.addAll(contents, inside);
        }

        int num = contents.size();

        String toss_inside = String.valueOf(num) + ".toss";
        String out_inside = String.valueOf(num) + ".out";

        while (contents.contains(toss_inside) || contents.contains(out_inside)) {
            num++;
            toss_inside = String.valueOf(num) + ".toss";
            out_inside = String.valueOf(num) + ".out";
        }
        return num;
    }

    public static File newMessage(String outbox_id, DraftMessage message) {
        File directory = getStationStorageDir(outbox_id);
        if (directory == null) return null;

        String path = directory.getAbsolutePath();

        int new_filename = getAndIncrementNumber(outbox_id);
        path += File.separator + String.valueOf(new_filename) + ".toss";

        File file = new File(path);
        try {
            Boolean r = file.createNewFile();
            if (!r) return null;
        } catch (IOException e) {
            e.printStackTrace();
            SimpleFunctions.debug(e.toString());
            return null;
        }
        if (!writeDraftToFile(file, message)) return null;
        return file;
    }
}