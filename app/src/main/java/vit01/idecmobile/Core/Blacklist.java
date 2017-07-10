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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import vit01.idecmobile.Strings;

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
        } else
            SimpleFunctions.debug(Strings.decorate(Strings.blacklist) + " " + Strings.empty_file_warning);
    }
}