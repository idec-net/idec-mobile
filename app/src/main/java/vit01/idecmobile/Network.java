package vit01.idecmobile;

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
    static String appName = "idecmobile";

    public static String getFile(Context context, String url, String data, int timeout) {
        SimpleFunctions.debug("fetch " + url);

        ConnectivityManager connMgr;
        NetworkInfo networkInfo;

        connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                return downloadUrl(url, data, timeout);
            } catch (Exception exception) {
                Log.w(appName, "Throw Exception: " + exception);
                exception.printStackTrace();
                return null;
            }
        } else return null;
    }

    public static String downloadUrl(String myurl, String data, int timeout) throws IOException {
        InputStream is = null;
        timeout *= 1000;
        try {
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

            Log.d(appName, "ServerResponse: " + response);
            is = conn.getInputStream();

            return SimpleFunctions.readIt(is);
        } finally {
            if (is != null) is.close();
        }
    }
}