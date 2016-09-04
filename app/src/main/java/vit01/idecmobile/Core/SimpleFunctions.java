package vit01.idecmobile.Core;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SimpleFunctions {
    public static String appName = "IDECMobile";
    public static ArrayList<String> emptyList = new ArrayList<>();
    public static Queue<String> debugMessages = new LinkedList<>();
    public static boolean debugTaskFinished = true;
    public static DateFormat simple_date = new SimpleDateFormat("dd.MM.yyyy, hh:mm");

    public static String join(String[] array, String delimiter) {
        String result = "";
        int stopLength = array.length - 1;

        for (int i = 0; i < stopLength; i++) {
            result += array[i] + delimiter;
        }
        result += array[stopLength];
        return result;
    }

    public static String read_internal_file(Context context, String filename) {
        try {
            FileInputStream is = context.openFileInput(filename);
            return readIt(is);
        } catch (Exception e) {
            return "";
        }
    }

    public static void write_internal_file(Context context, String filename, String data) {
        try {
            FileOutputStream os = context.openFileOutput(filename, Context.MODE_PRIVATE);
            os.write(data.getBytes(Charset.forName("UTF-8")));
            os.close();
        } catch (Exception e) {
            Log.e("WTF IDEC?", e.toString());
            e.printStackTrace();
        }
    }

    public static String readIt(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        final char[] buffer = new char[500];
        StringBuilder out = new StringBuilder();
        int read;

        do {
            read = reader.read(buffer, 0, buffer.length);
            if (read > 0) out.append(buffer, 0, read);
        }
        while (read >= 0);

        return new String(out);
    }

    public static String hsh(String str) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());
        } catch (Exception e) {
            Log.e("WTF?", e.toString());
            e.printStackTrace();
            return "No_SHA256?Seriously?";
        }
        return new String(Base64.encode(md.digest(), Base64.URL_SAFE),
                Charset.defaultCharset()).substring(0, 20);
    }

    public static ArrayList<String> ListDifference(ArrayList<String> first, ArrayList<String> second) {
        ArrayList<String> copy = (ArrayList<String>) first.clone();
        copy.removeAll(second);

        return copy;
    }

    public static <T> List<List<T>> chunks_divide(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }

    public static String[] List2Arr(List<String> list) {
        String[] newString = new String[list.size()];
        list.toArray(newString);
        return newString;
    }

    public static String timestamp2date(long unixtime) {
        return simple_date.format(new Date(unixtime * 1000));
    }

    public static void debug(String message) {
        Log.d(appName, message);

        if (!debugTaskFinished) {
            debugMessages.add(message);
        }
    }
}