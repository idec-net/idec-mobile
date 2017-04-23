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

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

public class IIMessage implements Serializable {
    public String id;
    public Hashtable<String, String> tags;
    public String echo;
    public long time;
    public String from, addr, to, subj, msg, repto;
    public boolean is_favorite, is_unread;

    public IIMessage() {
        id = null;
        echo = "no.echo";
        tags = new Hashtable<>();
        time = 0;
        from = "<nobody>";
        addr = "/dev/null";
        to = "<nobody>";
        subj = "no subj";
        msg = "no message";
        repto = null;
        is_favorite = false;
        is_unread = true;
    }

    public IIMessage(String rawmsg) {
        this();
        String[] elems = rawmsg.split("\n");

        if (elems.length >= 8) {
            tags = parseTags(elems[0]);
            echo = elems[1];
            time = Long.parseLong(elems[2]);
            from = elems[3];
            addr = elems[4];
            to = elems[5];
            subj = elems[6];

            msg = SimpleFunctions.
                    join(Arrays.copyOfRange(elems, 8, elems.length), "\n");

            repto = (tags.containsKey("repto")) ? tags.get("repto") : null;
            id = null;
            is_favorite = false;
            is_unread = true;
        }
    }

    static Hashtable<String, String> parseTags(String str) {
        if (str == null) return new Hashtable<>();

        String[] all_tags = str.split("/");
        Hashtable<String, String> new_tags = new Hashtable<>();

        for (int i = 0; i < all_tags.length; i += 2) {
            if (all_tags[i + 1] != null) {
                new_tags.put(all_tags[i], all_tags[i + 1]);
            } else {
                new_tags.put(all_tags[i], null);
            }
        }

        return new_tags;
    }

    static String collectTags(Hashtable<String, String> tags) {
        ArrayList<String> fragments = new ArrayList<>();

        Enumeration keys = tags.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String value = tags.get(key);

            if (value != null) fragments.add(key + "/" + value);
        }

        return TextUtils.join("/", fragments);
    }

    public String raw() {
        if (repto != null) tags.put("repto", repto);

        String stringTags = collectTags(tags);

        return stringTags + "\n" +
                echo + "\n" +
                String.valueOf(time) + "\n" +
                from + "\n" +
                addr + "\n" +
                to + "\n" +
                subj + "\n\n" + msg;
    }

    public String getTags() {
        return collectTags(tags);
    }
}
