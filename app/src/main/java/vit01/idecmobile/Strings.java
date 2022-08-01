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

package vit01.idecmobile;

import android.content.Context;

public class Strings {
    public static String
            empty_file_warning,
            create_file_error,
            file_write_error,
            blacklist,
            drafts_cache,
            echo_positions,
            node_mira,
            node_tavern,
            favorites,
            carbon,
            unread,
            search_results;

    static void initStrings(Context context) {
        empty_file_warning = context.getString(R.string.empty_file_warning);
        create_file_error = context.getString(R.string.create_file_error);
        file_write_error = context.getString(R.string.file_write_error);
        blacklist = context.getString(R.string.blacklist);
        drafts_cache = context.getString(R.string.drafts_cache);
        echo_positions = context.getString(R.string.echo_positions);
        node_mira = context.getString(R.string.node_mira);
        node_tavern = context.getString(R.string.node_tavern);

        favorites = context.getString(R.string.favorites);
        carbon = context.getString(R.string.carbon);
        unread = context.getString(R.string.unread);
        search_results = context.getString(R.string.search_results);
    }

    public static String decorate(String str) {
        return "[" + str + "]";
    }
}
