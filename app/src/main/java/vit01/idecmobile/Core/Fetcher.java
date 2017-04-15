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
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Fetcher {
    AbstractTransport transport;
    ArrayList<String> emptyList;

    public Fetcher(AbstractTransport db) {
        transport = db;
        emptyList = SimpleFunctions.emptyList;
        if (!Blacklist.loaded) Blacklist.loadBlacklist();
    }

    public static ArrayList<String> xfile_list_download(Context context, Station station) {
        ArrayList<String> result = new ArrayList<>();

        String toServer;

        try {
            toServer = "pauth=" + URLEncoder.encode(station.authstr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            SimpleFunctions.debug("Wrong encoding " + e.toString());
            return result;
        }

        String out = Network.getFile(context, station.address + "x/filelist", toServer, Config.values.connectionTimeout);
        if (out == null) return result;

        Collections.addAll(result, out.split("\n"));

        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).equals("")) result.remove(i);
        }

        return result;
    }

    public static boolean xfile_download(final Context context, Station station, String filename, File fileToSave) {
        String toServer;

        try {
            toServer = "pauth=" + URLEncoder.encode(station.authstr, "UTF-8") +
                    "&filename=" + URLEncoder.encode(filename, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            SimpleFunctions.debug("Wrong encoding " + e.toString());
            return false;
        }

        if (!fileToSave.exists()) {
            boolean created;
            try {
                created = fileToSave.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                SimpleFunctions.debug("Error while creating new file " + fileToSave.getAbsolutePath() + ": " + e.toString());
                return false;
            }
            if (!created)
                SimpleFunctions.debug("Failed to create new file" + fileToSave.getAbsolutePath());
            return false;
        }

        InputStream inputStream = Network.streamTransaction(context, station.address + "x/file", toServer, Config.values.connectionTimeout);
        if (inputStream == null) return false;

        FileOutputStream os;
        try {
            os = new FileOutputStream(fileToSave);
            final byte[] buffer = new byte[500];
            int totalRead = 0;

            int read;

            class progress implements Runnable {
                int currentvalue = 0;
                boolean joined = false;

                public void setCurrentvalue(int val) {
                    currentvalue = val;
                }

                @Override
                public void run() {
                    while (!joined) {
                        SimpleFunctions.debug(android.text.format.Formatter.formatFileSize(context, currentvalue));
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            progress prgr = new progress();
            Thread progressThread = new Thread(prgr);
            progressThread.start();

            do {
                read = inputStream.read(buffer, 0, buffer.length);
                if (read > 0) os.write(buffer, 0, read);
                totalRead += read;
                prgr.setCurrentvalue(totalRead);
            }
            while (read >= 0);

            prgr.joined = true;
            progressThread.join();

            os.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            SimpleFunctions.debug("Error writing to file " + e.toString());
            return false;
        }

        return true;
    }

    public ArrayList<String> fetch_messages(
            Context context,
            String address,
            ArrayList<String> firstEchoesToFetch,
            String xc_id,
            int one_request_limit,
            int fetch_limit,
            boolean pervasive_ue,
            int cut_remote_index,
            int timeout
    ) {
        // Слабо прочитать код всей этой функции на трезвую голову?
        // В общем, запасайтесь попкорном и алкоголем, ребята

        if (firstEchoesToFetch.size() == 0) return new ArrayList<>();

        ArrayList<String> echoesToFetch = new ArrayList<>();
        echoesToFetch.addAll(firstEchoesToFetch);

        if (xc_id != null) {
            String xc_cell_name = "xc_" + xc_id;
            String tmp_xc_cell_name = "xc_tmp_" + xc_id;

            String xc_url = address + "x/c/" + SimpleFunctions.join(
                    SimpleFunctions.List2Arr(firstEchoesToFetch), "/");

            String remote_xc_data = Network.getFile(context, xc_url, null, timeout);
            String local_xc_data = SimpleFunctions.read_internal_file(context, xc_cell_name);

            if (remote_xc_data == null) {
                SimpleFunctions.debug("Похоже, на станции проблема с /x/c");
                return null;
            }
            if (local_xc_data.equals(remote_xc_data)) return emptyList;

            boolean use_xc_now = true;
            ArrayList<String> excluded_echoareas = new ArrayList<>();

            if (local_xc_data.equals("")) {
                use_xc_now = false;
                // Если получили данные в первый раз, то /x/c пока не нужен
            }

            String[] local_xc_lines = local_xc_data.split("\n");
            String[] remote_xc_lines = remote_xc_data.split("\n");

            if (local_xc_lines.length != remote_xc_lines.length) {
                use_xc_now = false;
                // Значит пользователь просто обновил список эх. Продолжать не следует
            }

            if (use_xc_now) {
                Hashtable<String, Integer> remote_xc_dict = new Hashtable<>();
                Hashtable<String, Integer> local_xc_dict = new Hashtable<>();

                try {
                    xc_parse_values(remote_xc_dict, remote_xc_lines);
                    xc_parse_values(local_xc_dict, local_xc_lines);
                } catch (Exception e) {
                    SimpleFunctions.debug("Exception: " + e.toString());
                    return emptyList;
                }

                for (String echo : firstEchoesToFetch) {
                    int local_ts = 0, remote_ts = 0;

                    if (local_xc_dict.containsKey(echo)) {
                        local_ts = local_xc_dict.get(echo);
                    }

                    if (remote_xc_dict.containsKey(echo)) {
                        remote_ts = remote_xc_dict.get(echo);
                    }

                    if (remote_ts == local_ts) {
                        excluded_echoareas.add(echo);
                        SimpleFunctions.debug("Removed " + echo);
                    } else if (remote_ts > local_ts) {
                        if (remote_ts <= 0 || local_ts <= 0) continue;

                        int residual = remote_ts - local_ts;

                        if (fetch_limit > 0 && pervasive_ue && (residual > fetch_limit)) {
                            fetch_limit = residual;
                        }
                    }

                }
            }
            SimpleFunctions.write_internal_file(context, xc_cell_name, remote_xc_data);
            SimpleFunctions.write_internal_file(context, tmp_xc_cell_name, remote_xc_data); // чтобы лишний раз не выводить уведомления
            echoesToFetch = SimpleFunctions.ListDifference(echoesToFetch, excluded_echoareas);
        }

        if (echoesToFetch.size() == 0) return emptyList;

        String echoBundle;
        int bottomOffset = 0;

        if (fetch_limit != 0) {
            bottomOffset = fetch_limit;
            String offset = String.valueOf(bottomOffset);

            echoBundle = Network.getFile(context,
                    address + "u/e/" + SimpleFunctions.join(
                            SimpleFunctions.List2Arr(echoesToFetch), "/") +
                            "/-" + offset + ":" + offset, null, timeout);
        } else {
            echoBundle = Network.getFile(context,
                    address + "u/e/" + SimpleFunctions.join(
                            SimpleFunctions.List2Arr(echoesToFetch), "/"),
                    null, timeout);
        }

        Hashtable<String, ArrayList<String>> localIndex = new Hashtable<>();

        for (String echo : echoesToFetch) {
            SimpleFunctions.debug("Загрузка локальной эхи " + echo);

            localIndex.put(echo, transport.getMsgList(echo, 0, 0));
        }

        Hashtable<String, ArrayList<String>> remoteIndex = parseRemoteIndex(echoBundle);

        Hashtable<String, ArrayList<String>> commonDiff = new Hashtable<>();
        ArrayList<String> nextfetch = new ArrayList<>();

        for (String echo : echoesToFetch) {
            ArrayList<String> localMessages = localIndex.get(echo);
            ArrayList<String> remoteMessages = remoteIndex.get(echo);

            if (cut_remote_index > 0 && remoteMessages.size() > cut_remote_index) {
                int remLength = remoteMessages.size();
                remoteMessages = new ArrayList<>(
                        remoteMessages.subList(remLength - cut_remote_index, remLength));
                remoteIndex.put(echo, remoteMessages);
            }

            commonDiff.put(echo, SimpleFunctions.ListDifference(remoteMessages, localMessages));

            if (fetch_limit > 0 && pervasive_ue && remoteMessages.size() == commonDiff.get(echo).size()) {
                nextfetch.add(echo);
            }
        }

        // FETCHING IS MAGIC!

        while (nextfetch.size() > 0) {
            bottomOffset += fetch_limit;
            echoBundle = Network.getFile(context,
                    address + "u/e/" +
                            SimpleFunctions.join(SimpleFunctions.List2Arr(nextfetch), "/") +
                            "/-" + String.valueOf(bottomOffset) + ":" + String.valueOf(fetch_limit)
                    , null, timeout);

            Hashtable<String, ArrayList<String>> msgsDict = parseRemoteIndex(echoBundle);

            // Нельзя удалять элементы списка во время его итерации, иначе будет
            // ConcurrentModificationException, поэтому список nextfetch копируем

            List<String> nextfetch_copy = new ArrayList<>(nextfetch);
            for (String echo : nextfetch_copy) {
                ArrayList<String> localMessages = localIndex.get(echo);
                ArrayList<String> remoteMessages = msgsDict.get(echo);

                if (remoteMessages == null || remoteMessages.size() == 0) {
                    nextfetch.remove(echo);
                    continue;
                }

                ArrayList<String> diff = SimpleFunctions.ListDifference(remoteMessages, localMessages);
                diff = SimpleFunctions.ListDifference(diff, commonDiff.get(echo));

                ArrayList<String> sumdiff = new ArrayList<>(diff);

                sumdiff.addAll(diff);
                sumdiff.addAll(commonDiff.get(echo));

                commonDiff.put(echo, sumdiff);

                if (remoteMessages.size() != diff.size()) {
                    nextfetch.remove(echo);
                }
            }
        }

        Hashtable<String, String> echoForMsgid = new Hashtable<>();
        List<String> fetchedEchoes = Collections.list(commonDiff.keys());
        ArrayList<String> difference = new ArrayList<>();

        for (String echo : fetchedEchoes) {
            ArrayList<String> msglist = commonDiff.get(echo);
            difference.addAll(msglist);

            for (String msgid : msglist) {
                echoForMsgid.put(msgid, echo);
            }
        }

        SimpleFunctions.debug("Применяем чёрный список к индексу...");
        difference.removeAll(Blacklist.badMsgids);
        List<List<String>> difference2d = SimpleFunctions.chunks_divide(difference, one_request_limit);

        ArrayList<String> savedMessages = new ArrayList<>();

        for (List<String> diff : difference2d) {
            String fullBundle = Network.getFile(context,
                    address + "u/m/" + SimpleFunctions.join(
                            SimpleFunctions.List2Arr(diff), "/"), null, timeout);

            if (fullBundle == null) {
                SimpleFunctions.debug("Ошибка получения бандла сообщений. Проверь интернет");
                if (savedMessages.size() > 0) return savedMessages;
                else return null;
            }

            ArrayList<String> bundles = new ArrayList<>();
            bundles.addAll(Arrays.asList(fullBundle.split("\n")));

            for (String bundle : bundles) {
                String[] pieces = bundle.split(":");

                if (pieces.length == 2 && !pieces[0].equals("") && !pieces[1].equals("")) {
                    String msgid = pieces[0];
                    String message;
                    String echo = echoForMsgid.get(msgid);

                    SimpleFunctions.debug("savemsg " + msgid + " to " + echo);
                    try {
                        byte[] rawmsg = Base64.decode(pieces[1], Base64.DEFAULT);
                        message = new String(rawmsg, "UTF-8");
                        transport.saveMessage(msgid, echo, message);
                        savedMessages.add(msgid);
                    } catch (Exception e) {
                        e.printStackTrace();
                        SimpleFunctions.debug("Invalid decoded message: " + pieces[1]);
                    }
                } else {
                    SimpleFunctions.debug("Wrong message bundle: " + bundle);
                }
            }
        }

        return savedMessages;
    }

    private void xc_parse_values(Hashtable<String, Integer> htable, String[] lines) {
        for (String line : lines) {
            String[] pieces = line.split(":");
            if (pieces.length < 2) continue;

            int value = Integer.parseInt(pieces[1]);

            htable.put(pieces[0], value);
        }
    }

    private Hashtable<String, ArrayList<String>> parseRemoteIndex(String echoBundle) {
        Hashtable<String, ArrayList<String>> remoteParsedEchos = new Hashtable<>();

        String lastEcho = "";

        for (String element : echoBundle.split("\n")) {
            if (!element.equals("")) {
                if (!element.contains(".")) {
                    remoteParsedEchos.get(lastEcho).add(element);
                } else {
                    lastEcho = element;
                    remoteParsedEchos.put(lastEcho, new ArrayList<String>());
                }
            }
        }

        return remoteParsedEchos;
    }
}
