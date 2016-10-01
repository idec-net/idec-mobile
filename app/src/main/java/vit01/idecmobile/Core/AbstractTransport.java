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
    int countUnread(String echo);

    ArrayList<String> getFavorites();

    // Это для работы карбонки (классической)!
    ArrayList<String> messagesToUsers(List<String> users, int limit, boolean unread);

    ArrayList<String> getUnreadEchoareas();
    ArrayList<String> getUnreadMessages(String echoarea);
    ArrayList<String> getAllUnreadMessages();

    ArrayList<String> getUnreadFavorites();

    void setUnread(boolean unread, List<String> msgids);

    void setUnread(boolean unread, String area);

    void setFavorite(boolean favorite, List<String> msgids);
}