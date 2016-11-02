package vit01.idecmobile.Core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Blacklist {
    public static String filename = "blacklist.txt";
    public static ArrayList<String> badMsgids = new ArrayList<>();
    public static boolean loaded = false;

    public static void loadBlacklist() {
        loaded = true;
        ExternalStorage.initStorage();
        File blacklist_file = new File(ExternalStorage.rootStorage, filename);
        badMsgids.clear();

        if (blacklist_file.exists() && blacklist_file.canRead()) {
            try {
                String[] rawLines = SimpleFunctions.readIt(new FileInputStream(blacklist_file)).split("\n");
                if (rawLines.length > 0) badMsgids.addAll(Arrays.asList(rawLines));
                badMsgids.remove("");
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug(e.toString());
            }
        }
        SimpleFunctions.debug("ЧС пуст, либо недоступен на чтение");
    }
}