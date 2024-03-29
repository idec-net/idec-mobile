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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import vit01.idecmobile.Strings;

public class EchoReadingPosition {
    // Этот класс нужен, чтобы запоминать последнее прочитанное сообщение
    // в каждой эхе, дабы продолжать чтение с того же места
    // Хранит данные в собственном формате, в каталоге приложения на SD-карте

    public static String filename = "positions.cache";
    public static File positions_file;
    public static HashMap<String, String> positions = null;

    public static void getPositionCache() {
        if (positions == null) positions = new HashMap<>();
        else positions.clear();

        ExternalStorage.initStorage();
        positions_file = new File(ExternalStorage.rootStorage, filename);

        if (positions_file.exists() && positions_file.canRead()) {
            try {
                String[] rawLines = SimpleFunctions.readIt(new FileInputStream(positions_file)).split("\n");
                if (rawLines.length > 0) {
                    for (String line : rawLines) {
                        String[] pieces = line.split(":");
                        if (pieces.length > 1) {
                            positions.put(pieces[0], pieces[1]);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug(e.toString());
            }
        } else
            SimpleFunctions.debug(Strings.decorate(Strings.echo_positions) + " " + Strings.empty_file_warning);
    }

    public static void writePositionCache() {
        if (positions_file == null || positions == null) return;

        if (!positions_file.exists()) {
            try {
                boolean r = positions_file.createNewFile();
                if (!r) {
                    SimpleFunctions.debug(Strings.create_file_error + Strings.decorate(Strings.echo_positions));
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug(e.toString());
                return;
            }
        }
        if (positions_file.canWrite()) {
            FileOutputStream os;
            try {
                os = new FileOutputStream(positions_file);

                for (String echo : positions.keySet()) {
                    String value = positions.get(echo);
                    os.write((echo + ":" + value + "\n").getBytes());
                }

                os.close();
            } catch (IOException e) {
                SimpleFunctions.debug(Strings.file_write_error + " " + Strings.decorate(Strings.echo_positions));
            }
        }
    }

    public static String getPosition(String echoarea) {
        if (positions == null) getPositionCache();
        if (positions.containsKey(echoarea)) return positions.get(echoarea);
        else return null;
    }

    public static void setPosition(String echoarea, String msgid) {
        if (positions == null) getPositionCache();
        positions.put(echoarea, msgid);
    }
}