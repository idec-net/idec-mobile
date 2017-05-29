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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

import info.guardianproject.netcipher.NetCipher;
import vit01.idecmobile.prefs.Config;

public class Network {
    public static Proxy proxy = null;

    public static String getFile(Context context, String url, String data, int timeout) {
        InputStream stream = streamTransaction(context, url, data, timeout);
        if (stream == null) return null;

        try {
            String result = SimpleFunctions.readIt(stream);
            stream.close();
            return result;
        } catch (IOException e) {
            SimpleFunctions.debug("Throw Exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    public static InputStream streamTransaction(Context context, String url, String data, int timeout) {
        SimpleFunctions.debug("fetch " + url);

        ConnectivityManager connMgr;
        NetworkInfo networkInfo;

        connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                if (proxy == null) setupProxy();
                return getInputStream(url, data, timeout);
            } catch (Exception exception) {
                SimpleFunctions.debug("Throw Exception: " + exception);
                exception.printStackTrace();
                return null;
            }
        } else return null;
    }

    public static InputStream getInputStream(String myurl, String data, int timeout) throws IOException {
        InputStream is;
        timeout *= 1000;

        HttpURLConnection conn = NetCipher.getHttpURLConnection(myurl);

        conn.setReadTimeout(timeout);
        conn.setConnectTimeout(timeout);

        if (data == null) {
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
        } else {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
        }

        conn.connect();
        int response = conn.getResponseCode();

        Log.d(SimpleFunctions.appName, "ServerResponse: " + response);
        is = conn.getInputStream();

        return is;
    }

    public static void setupProxy() {
        if (!Config.values.useProxy) {
            SimpleFunctions.debug("(Прокси сервер: не используется)");
            Authenticator.setDefault(null);
            proxy = Proxy.NO_PROXY;
            NetCipher.setProxy(proxy);
            return;
        }

        Proxy.Type p_type = Proxy.Type.HTTP;

        URI uri = null;
        try {
            uri = new URI("http://" + Config.values.proxyAddress);
            SimpleFunctions.debug("Используем прокси " + uri.getHost() + " " + uri.getPort() + " " + uri.getUserInfo());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            SimpleFunctions.debug(e.toString());
            SimpleFunctions.debug("Ошибка парсинга адреса прокси-сервера!");
        }

        String host = (uri != null) ? uri.getHost() : "127.0.0.1";
        int port = (uri != null) ? uri.getPort() : 8118;

        proxy = new Proxy(p_type, new InetSocketAddress(host, port));
        NetCipher.setProxy(proxy);

        String userInfo = (uri != null) ? uri.getUserInfo() : null;

        if (userInfo == null) {
            Authenticator.setDefault(null);
        } else {
            String[] pieces = userInfo.split(":");
            String username = null;
            String pass = "none";

            if (pieces.length == 1) username = userInfo;
            else if (pieces.length == 2) {
                username = pieces[0];
                pass = pieces[1];
            }

            final String finalPass = pass;
            final String finalUsername = username;
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(finalUsername, finalPass.toCharArray());
                        }
                    }
            );

            if (username == null) username = "";

            System.setProperty("http.proxyUser", username);
            System.setProperty("http.proxyPassword", finalPass);
        }
    }
}