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
import java.util.Hashtable;

public class FEchoFile {
    public String id, fecho, filename, addr, description;
    public Hashtable<String, String> tags;
    public long serverSize;

    public FEchoFile() {
        id = null;
        fecho = "no.echo";
        filename = null;
        addr = "/dev/null";
        description = "<empty description>";
        tags = new Hashtable<>();
        serverSize = 0;
    }

    public FEchoFile(String raw) {
        this();

        String[] pieces = raw.split(":");

        if (pieces.length < 5) return;

        id = pieces[0];
        filename = pieces[1];
        serverSize = Long.parseLong(pieces[2]);
        addr = pieces[3];
        description = pieces[4];

        if (pieces.length > 5) {
            for (int i = 5; i < pieces.length; i++) {
                description += ":" + pieces[i];
            }
        }
    }

    public FEchoFile(String raw, String echo, String strtags) {
        this(raw);
        fecho = echo;
        tags = IIMessage.parseTags(strtags);
    }

    public String raw() {
        String strid = id == null ? "null" : id;
        String strfilename = filename == null ? "/dev/null" : filename;
        String strsize = String.valueOf(serverSize);

        return strid + ":" + strfilename + ":" + strsize + ":" + addr + ":" + description;
    }

    private boolean existsLocally(File file) {
        if (file == null) file = getLocalFile();
        return file != null && file.exists() && file.isFile();
    }

    private boolean existsLocally() {
        return existsLocally(null);
    }

    public boolean localSizeIsCorrect() {
        File file = getLocalFile();
        return file != null && existsLocally(file) && file.length() == serverSize;
    }

    private File getLocalFile() {
        if (id == null || filename == null) return null;

        File idDir = new File(ExternalStorage.fileStorage, id);

        if (!idDir.exists()) {
            boolean md = idDir.mkdirs();
            if (!md) {
                SimpleFunctions.debug("failed to create file container dir for " + id);
                return null;
            }
        } else if (!idDir.isDirectory()) {
            SimpleFunctions.debug("fid dir " + id + " is not a directory");
            return null;
        }

        return new File(idDir, id);
    }
}