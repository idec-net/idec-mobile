package vit01.idecmobile.Core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {
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
                return getInputStream(url, data, timeout);
            } catch (Exception exception) {
                SimpleFunctions.debug("Throw Exception: " + exception);
                exception.printStackTrace();
                return null;
            }
        } else return null;
    }

    public static InputStream getInputStream(String myurl, String data, int timeout) throws IOException {
        InputStream is = null;
        timeout *= 1000;

        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
}