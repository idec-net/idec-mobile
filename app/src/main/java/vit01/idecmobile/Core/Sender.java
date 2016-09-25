package vit01.idecmobile.Core;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Sender {
    public static int sendMessages(Context context) {
        int countsent = 0;

        for (Station station : Config.values.stations) {
            File tossesDir = DraftStorage.getStationStorageDir(station.outbox_storage_id);
            ArrayList<File> contents = DraftStorage.getFilesInside(tossesDir, true);

            if (contents.size() == 0) continue;

            for (File file : contents) {
                String str;

                try {
                    FileInputStream fis = new FileInputStream(file);
                    str = SimpleFunctions.readIt(fis);
                    fis.close();
                } catch (Exception e) {
                    SimpleFunctions.debug(e.toString());
                    e.printStackTrace();
                    continue;
                }

                String base64str = new String(Base64.encode(str.getBytes(), Base64.DEFAULT));
                String toServer = "";

                try {
                    toServer = "tmsg=" +
                            URLEncoder.encode(base64str) +
                            "&pauth=" + URLEncoder.encode(station.authstr, "UTF-8");

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    SimpleFunctions.debug("Wrong encoding " + e.toString());
                }

                String out = Network.getFile(context, station.address + "u/point", toServer, Config.values.connectionTimeout);
                SimpleFunctions.debug(out);

                if (out != null && out.startsWith("msg ok")) {
                    countsent++;
                    String filename_2 = file.getName();
                    filename_2 = filename_2.substring(0, filename_2.length() - 4) + "out";

                    File file2 = new File(file.getParentFile(), filename_2);
                    boolean rename = file.renameTo(file2);

                    if (!rename)
                        SimpleFunctions.debug("Проблема с переименованием в " + filename_2);
                }
            }
        }

        return countsent;
    }
}
