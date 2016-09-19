package vit01.idecmobile.Core;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;

public class DraftStorage {
    static String dataDirectory = "idecMobile";
    static File rootStorage = Environment.getExternalStoragePublicDirectory(dataDirectory);
    static FilenameFilter draftsFilter;
    static FilenameFilter sentFilter;

    public DraftStorage() {
        if (!rootStorage.mkdirs()) {
            SimpleFunctions.debug("Root directory for drafts not created!");
        }

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
    }

    public static File getStationStorageDir(String outbox_id) {
        File file = new File(rootStorage, outbox_id);
        if (!file.mkdirs()) {
            SimpleFunctions.debug("Directory not created");
        }
        return file;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static ArrayList<File> getFilesInside(File directory, boolean unsent) {
        FilenameFilter filter = (unsent) ? draftsFilter : sentFilter;

        File[] contents = directory.listFiles(filter);
        ArrayList<File> result = new ArrayList<>();
        Collections.addAll(result, contents);

        return result;
    }

    public static ArrayList<File> getAllEntries(boolean unsent) {
        ArrayList<File> result = new ArrayList<>();

        for (Station station : Config.values.stations) {
            File stationDir = getStationStorageDir(station.outbox_storage_id);
            ArrayList<File> current = getFilesInside(stationDir, unsent);
            result.addAll(current);
        }

        return result;
    }
}