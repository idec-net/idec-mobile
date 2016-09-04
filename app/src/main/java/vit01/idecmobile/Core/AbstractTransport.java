package vit01.idecmobile.Core;

import java.util.ArrayList;
import java.util.Hashtable;

public interface AbstractTransport {
    boolean saveMessage(String msgid, String echo, IIMessage message);

    boolean saveMessage(String msgid, String echo, String rawmessage);

    boolean updateMessage(String msgid, IIMessage message);

    boolean updateMessage(String msgid, String rawmessage);

    boolean deleteMessage(String msgid, String echo);

    void deleteMessages(ArrayList<String> msgids, String echo);

    ArrayList<String> getMsgList(String echo, int offset, int length);

    void deleteEchoarea(String echo, boolean with_contents);

    String getRawMessage(String msgid);

    Hashtable<String, String> getRawMessages(ArrayList<String> msgids);

    IIMessage getMessage(String msgid);

    Hashtable<String, IIMessage> getMessages(ArrayList<String> msgids);

    ArrayList<String> fullEchoList();

    int countMessages(String echo);
}