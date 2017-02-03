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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public interface AbstractTransport {
    boolean saveMessage(String msgid, String echo, IIMessage message);

    boolean saveMessage(String msgid, String echo, String rawmessage);

    boolean updateMessage(String msgid, IIMessage message);

    boolean updateMessage(String msgid, String rawmessage);

    boolean deleteMessage(String msgid, String echo);

    void deleteMessages(ArrayList<String> msgids, String echo);

    ArrayList<String> getMsgList(String echo, int offset, int length);

    void deleteEchoarea(String echo, boolean with_contents);
    void FuckDeleteEverything();

    String getRawMessage(String msgid);
    Hashtable<String, String> getRawMessages(ArrayList<String> msgids);

    IIMessage getMessage(String msgid);
    Hashtable<String, IIMessage> getMessages(ArrayList<String> msgids);

    ArrayList<String> fullEchoList();

    int countMessages(String echo);
    int countUnread();
    int countFavorites();

    // Это для работы карбонки (классической)!
    ArrayList<String> messagesToUsers(List<String> users, int limit, boolean unread);

    ArrayList<String> getUnreadMessages(String echoarea);
    ArrayList<String> getAllUnreadMessages();

    echoStat getUnreadStats(String echoarea);

    ArrayList<String> getFavorites();
    ArrayList<String> getUnreadFavorites();

    void setUnread(boolean unread, List<String> msgids);
    void setUnread(boolean unread, String area);

    void setFavorite(boolean favorite, List<String> msgids);

    class echoStat {
        public int total_count = 0, unread_count = 0;
    }
}