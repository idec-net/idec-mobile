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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Strings;

public class Station implements Serializable {
    static final long serialVersionUID = 100L;

    public String
            nodename = Strings.node_tavern,
            address = "http://idec.spline-online.tk/",
            authstr = "",
            outbox_storage_id = null;
    public ArrayList<String> echoareas = new ArrayList<>();
    public ArrayList<String> file_echoareas = new ArrayList<>();
    public boolean
            fetch_enabled = true,
            xc_enable = true,
            advanced_ue = true,
            pervasive_ue = false,
            fecho_support = false;
    public int
            ue_limit = 25,
            cut_remote_index = 50;

    public Station() {
        String[] default_echoareas = new String[]{"pipe.2032", "idec.talks", "std.club", "ii.test.14", "develop.16", "linux.14"};

        String[] default_file_echoareas = new String[]{"pictures", "music", "books", "books.tech"};

        Collections.addAll(echoareas, default_echoareas);
        Collections.addAll(file_echoareas, default_file_echoareas);
        outbox_storage_id = SimpleFunctions.getRandomUUID();
    }
}
