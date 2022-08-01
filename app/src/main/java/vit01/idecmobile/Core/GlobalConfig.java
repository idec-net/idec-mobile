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

import vit01.idecmobile.Strings;

public class GlobalConfig implements Serializable {
    static final long serialVersionUID = 1L;
    public boolean
            defaultEditor = true,
            firstRun = true,
            useProxy = false,
            useTor = false,
            oldQuote = false, // упрощённое (старое) цитирование
            notificationsEnabled = false,
            notificationsVibrate = true,
            swipeToFetch = true,
            hide_toolbar_when_scrolling = false,
            disableMsglist = true, // начать читать эху сразу с того же места
            sortByDate = true,
            openUnreadAfterFetch = false,
            autoFetchEnabled = false;
    public int
            fecho_sort_type = 0,
            oneRequestLimit = 20,
            connectionTimeout = 20,
            carbon_limit = 50, // максимальное количество сообщений в карбонке
            notifyFireDuration = 15, // интервал проверки для уведомлений (минуты)
            proxyType = 1; // 0 - Socks, 1 - HTTP
    public ArrayList<String> offlineEchoareas = new ArrayList<>();
    public ArrayList<Station> stations = new ArrayList<>();

    // Сообщения какого пользователя слать в карбонку
    public String carbon_to = "All", // разделять двоеточием
            proxyAddress = "127.0.0.1:8118", // аутентификация для http-прокси поддерживается
            applicationTheme = "default", // тема оформления
            textsignature = ""; // текстовая подпись

    public GlobalConfig() {
        offlineEchoareas.add("lenta.rss");
        offlineEchoareas.add("edgar.allan.poe");

        stations.add(new Station());
    }
}