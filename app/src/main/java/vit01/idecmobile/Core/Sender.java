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

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import vit01.idecmobile.prefs.Config;

public class Sender {
    public static Boolean sendOneMessage(Context context, Station station, File file, boolean force) {
        // параметр force нужен, чтобы отправлять черновики принудительно
        String str, toServer;

        try {
            FileInputStream fis = new FileInputStream(file);
            str = SimpleFunctions.readIt(fis);
            fis.close();
        } catch (Exception e) {
            SimpleFunctions.debug(e.toString());
            e.printStackTrace();
            return false;
        }

        if (!force) {
            String hash = SimpleFunctions.hsh(str);
            if (DraftsValidator.hashExists(hash)) {
                SimpleFunctions.debug("Удаляем пустой черновик...");
                boolean d = file.delete();
                if (d) {
                    DraftsValidator.deleteHash(hash);
                } else SimpleFunctions.debug("Ошибка при удалении черновика");

                return null;
            }
        }

        String base64str = new String(Base64.encode(str.getBytes(), Base64.DEFAULT));

        try {
            toServer = "tmsg=" +
                    URLEncoder.encode(base64str) +
                    "&pauth=" + URLEncoder.encode(station.authstr, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            SimpleFunctions.debug("Wrong encoding " + e.toString());
            return false;
        }

        String out = Network.getFile(context, station.address + "u/point", toServer, Config.values.connectionTimeout);
        if (out != null) SimpleFunctions.debug(out);

        if (out != null && out.startsWith("msg ok")) {
            String filename_2 = file.getName();
            filename_2 = filename_2.substring(0, filename_2.length() - 4) + "out";

            File file2 = new File(file.getParentFile(), filename_2);
            boolean rename = file.renameTo(file2);

            if (!rename)
                SimpleFunctions.debug("Проблема с переименованием в " + filename_2);
            return true;
        }
        return false;
    }

    public static int sendMessages(Context context) {
        int countsent = 0;
        int countdeleted = 0;
        int totaldrafts = 0;

        for (Station station : Config.values.stations) {
            File tossesDir = ExternalStorage.getStationStorageDir(station.outbox_storage_id);
            ArrayList<File> contents = ExternalStorage.getDraftsInside(tossesDir, true);

            if (contents.size() == 0) continue;
            totaldrafts += contents.size();

            for (File file : contents) {
                Boolean sent = sendOneMessage(context, station, file, false);
                if (sent != null) {
                    if (sent) countsent++;
                } else countdeleted++;
            }
        }

        if (totaldrafts > 0 && (countsent + countdeleted) == totaldrafts) {
            // Значит мы отправили все черновики, которые у нас есть. Чистим кэш
            DraftsValidator.deleteAll();
        }

        return countsent;
    }
}