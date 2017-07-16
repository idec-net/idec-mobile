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

import vit01.idecmobile.prefs.Config;

public class GlobalTransport {
    public static AbstractTransport transport;

    public static AbstractTransport transport(Context context) {
        if (transport == null) transport = new SqliteTransport(context);
        return transport;
    }

    public static AbstractTransport transport() {
        if (transport == null && Config.lastContext != null)
            transport = transport(Config.lastContext);
        return transport;
    }
}
