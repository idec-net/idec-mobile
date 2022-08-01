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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Hashtable;

import info.guardianproject.netcipher.NetCipher;
import vit01.idecmobile.ProgressActivity;
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

    static InputStream streamTransaction(Context context, String url, String data, int timeout) {
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

    private static InputStream getInputStream(String myurl, String data, int timeout) throws IOException {
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

    public static InputStream performFileUpload(ProgressActivity.upload_fp uploader,
                                                String myurl, InputStream fis, Hashtable<String, String> data,
                                                String form_file_key, String filename, int timeout)
            throws IOException {
        timeout *= 1000;
        HttpURLConnection conn = NetCipher.getHttpURLConnection(myurl);

        conn.setReadTimeout(timeout);
        conn.setConnectTimeout(timeout);
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        String boundary = "===" + System.currentTimeMillis() + "===";

        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream os = conn.getOutputStream();

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os), true);

        for (String key : data.keySet()) {
            String value = data.get(key);

            writer.append(String.format("--%s\r\n", boundary));
            writer.append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n", key));
            writer.append("Content-Type: text/plain; charset=UTF-8\r\n");
            writer.append("\r\n");
            writer.append(value).append("\r\n");
            writer.flush();
        }

        writer.append(String.format("--%s\r\n", boundary));
        writer.append(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n",
                form_file_key, filename));
        writer.append(String.format("Content-Type: %s\r\n", URLConnection.guessContentTypeFromName(filename)));
        writer.append("Content-Transfer-Encoding: binary\r\n");
        writer.append("\r\n");
        writer.flush();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
            uploader.bytesdone += bytesRead;
        }
        os.flush();
        fis.close();

        writer.append("\r\n");
        writer.flush();

        writer.append("\r\n").flush();
        writer.append(String.format("--%s--\r\n", boundary));
        writer.close();

        conn.connect();
        int response = conn.getResponseCode();

        Log.d(SimpleFunctions.appName, "ServerResponse upload: " + response);
        return conn.getInputStream();
    }

    private static void setupProxy() {
        if (!Config.values.useProxy) {
            SimpleFunctions.debug("(Not using proxy servers this time)");
            Authenticator.setDefault(null);
            proxy = Proxy.NO_PROXY;
            NetCipher.setProxy(proxy);
            return;
        }

        Proxy.Type p_type = Proxy.Type.HTTP;

        URI uri = null;
        try {
            uri = new URI("http://" + Config.values.proxyAddress);
            SimpleFunctions.debug("Using proxy: " + uri.getHost() + " " + uri.getPort() + " " + uri.getUserInfo());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            SimpleFunctions.debug(e.toString());
            SimpleFunctions.debug("Error parsing proxy server address!");
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