/*
 * Copyright (c) 2016-2022 Viktor Fedenyov <me@alicorn.tk> <https://alicorn.tk>
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

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Strings;

public class DraftsValidator {
    // Эта штука нужна, чтобы проверять, был изменён созданный черновик или нет
    // Если он изменён не был, значит пользователь ошибся, и черновик удаляем

    public static String filename = "drafts-hashes.cache";
    public static File hashes_file;
    public static ArrayList<String> hashes = null;

    public static void getHashesCache() {
        if (hashes == null) hashes = new ArrayList<>();
        else hashes.clear();

        ExternalStorage.initStorage();
        hashes_file = new File(ExternalStorage.rootStorage, filename);

        if (hashes_file.exists() && hashes_file.canRead()) {
            try {
                String[] rawLines = SimpleFunctions.readIt(new FileInputStream(hashes_file)).split("\n");
                if (rawLines.length > 0) {
                    Collections.addAll(hashes, rawLines);
                }
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug(e.toString());
            }
        } else
            SimpleFunctions.debug(Strings.decorate(Strings.drafts_cache) + " " + Strings.empty_file_warning);
    }

    public static void writeHashesCache() {
        if (!hashes_file.exists()) {
            try {
                boolean r = hashes_file.createNewFile();
                if (!r) {
                    SimpleFunctions.debug(Strings.create_file_error + Strings.decorate(Strings.drafts_cache));
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug(e.toString());
                return;
            }
        }
        if (hashes_file.canWrite()) {
            FileOutputStream os;
            try {
                os = new FileOutputStream(hashes_file);
                String data = (hashes.size() > 0) ? TextUtils.join("\n", hashes) : "";

                os.write(data.getBytes());
                os.close();
            } catch (IOException e) {
                SimpleFunctions.debug(Strings.file_write_error + " " + Strings.decorate(Strings.drafts_cache));
            }
        }
    }

    public static boolean hashExists(String hash) {
        if (hashes == null) getHashesCache();
        return hashes.contains(hash);
    }

    public static void appendHash(String hash) {
        if (hashes == null) getHashesCache();

        if (!hashes.contains(hash)) {
            hashes.add(hash);
            writeHashesCache();
        }
    }

    public static void deleteHash(String hash) {
        if (hashes == null) getHashesCache();
        if (hash == null) return;

        if (hashes.contains(hash)) {
            hashes.remove(hash);
            writeHashesCache();
        }
    }

    public static void deleteAll() {
        if (hashes == null) getHashesCache();
        if (hashes.size() > 0) {
            hashes.clear();
            writeHashesCache();
        }
    }

    public static String getLastHash() {
        if (hashes == null) getHashesCache();

        if (hashes.size() > 0) {
            return hashes.get(hashes.size() - 1);
        }
        else return null;
    }
}