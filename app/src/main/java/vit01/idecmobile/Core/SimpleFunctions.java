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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;

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
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class SimpleFunctions {
    public static String appName = "IDECMobile";
    public static ArrayList<String> emptyList = new ArrayList<>();
    public static Queue<String> debugMessages = new LinkedList<>();
    public static Queue<String> prettyDebugMessages = new LinkedList<>();
    public static boolean debugTaskFinished = true;
    public static DateFormat full_date = new SimpleDateFormat("dd.MM.yyyy (E), HH:mm", Locale.getDefault());
    public static Pattern quote_pattern = Pattern.compile("(^\\s?[\\w_.а-яА-Я0-9\\-]{0,20})((>)+)(.+$)",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public static Pattern comment_pattern = Pattern.compile("(^|(\\w\\s+))(//|#)(.+$)", Pattern.MULTILINE);
    public static Pattern PS_pattern = Pattern.compile("^(PS|P.S|ЗЫ|З.Ы)(.+$)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public static Pattern ii_link_pattern = Pattern.compile("ii://(\\w[\\w.-]+\\w+)");
    public static Pattern url_pattern = Pattern.compile("(https?|ftp|file)://?[-A-Za-zА-Яа-я0-9+&@#/%?=~_|!:,.;]+[-A-Za-zА-Яа-я0-9+&@#/%=~_|]",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public static Pattern mailto_pattern = Patterns.EMAIL_ADDRESS;
    public static String commentColor, PSColor, quoteColor;

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
        ArrayList<String> copy = new ArrayList<>(first);
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

    public static String timestamp2date(long unixtime) {
        return full_date.format(new Date(unixtime * 1000));
    }

    public static String reparseMessage(Context context, String msg) {
        // Вот эти первые строки - костыль

        if (quoteColor == null)
            quoteColor = String.format("#%06X", (0xFFFFFF & SimpleFunctions.colorFromTheme(context, R.attr.quoteColor)));

        if (PSColor == null)
            PSColor = String.format("#%06X", (0xFFFFFF & SimpleFunctions.colorFromTheme(context, R.attr.PSColor)));

        if (commentColor == null)
            commentColor = String.format("#%06X", (0xFFFFFF & SimpleFunctions.colorFromTheme(context, R.attr.commentColor)));

        msg = msg.replaceAll("<", "&lt;");
        Matcher quote_match = quote_pattern.matcher(msg);
        msg = quote_match.replaceAll(String.format("<font color='%s'>$0</font>", quoteColor));

        Matcher comment_match = comment_pattern.matcher(msg);
        msg = comment_match.replaceAll(String.format("$1<font color='%s'>$3$4</font>", commentColor));

        Matcher PS_match = PS_pattern.matcher(msg);
        msg = PS_match.replaceAll(String.format("<font color='%s'>$0</font>", PSColor));

        Matcher ii_link_match = ii_link_pattern.matcher(msg);
        msg = ii_link_match.replaceAll("<a href=\"$0\">$0</a>");

        Matcher url_match = url_pattern.matcher(msg);
        msg = url_match.replaceAll("<a href=\"$0\">$0</a>");

        Matcher mailto_match = mailto_pattern.matcher(msg);
        msg = mailto_match.replaceAll("<a href=\"mailto:$0\">$0</a>");

        String[] strings = msg.split("\n");
        ArrayList<String> result = new ArrayList<>();
        boolean pre_flag = false;

        for (String piece : strings) {
            if (piece.equals("====")) {
                if (!pre_flag) {
                    pre_flag = true;
                    result.add("<font face='monospace'>====");
                } else {
                    pre_flag = false;
                    result.add("====</font>");
                }
            } else {
                if (pre_flag) result.add(piece.replaceAll(" ", "&#160;")
                        .replaceAll("\t", "&#160;&#160;&#160;&#160;"));
                else result.add(piece);
            }
        }
        if (pre_flag) strings[strings.length - 1] += "</font>";

        return TextUtils.join("<br>", result);
    }

    public static void resetIDECParserColors() {
        // Следующие строчки нужны, чтобы парсер перезагрузил цвета из темы оформления
        SimpleFunctions.commentColor = null;
        SimpleFunctions.PSColor = null;
        SimpleFunctions.quoteColor = null;
    }

    public static String messagePreview(String text) {
        Matcher quote_matcher = quote_pattern.matcher(text);
        text = quote_matcher.replaceAll("");
        text = text.replaceAll("\n(\n)+", "\n");

        return text.trim();
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String subjAnswer(String subj) {
        if (!subj.startsWith("Re:")) {
            return "Re: " + subj;
        } else return subj;
    }

    public static String quoteAnswer(String message, String user, Boolean old) {
        String[] pieces;
        if (old) {
            pieces = message.split("\n");
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i].trim().equals("")) continue;

                if (pieces[i].contains(">")) pieces[i] = ">" + pieces[i];
                else pieces[i] = "> " + pieces[i];
            }
        } else {
            String quoted_user = "";
            String[] user_pieces = user.split(" ");

            if (user_pieces.length > 1) {
                for (String piece : user_pieces) {
                    quoted_user += piece.charAt(0);
                }
            } else {
                quoted_user = user;
            }

            pieces = message.split("\n");
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i].trim().equals("")) continue;

                Matcher quote_match = quote_pattern.matcher(pieces[i]);
                if (quote_match.matches()) {
                    pieces[i] = quote_match.replaceAll("$1>$2$4");
                } else {
                    pieces[i] = quoted_user + "> " + pieces[i];
                }
            }
        }
        return TextUtils.join("\n", pieces);
    }

    public static int getPreferredOutboxId(String echoarea) {
        int nodeindex = Config.currentSelectedStation;

        if (Config.values.stations.get(nodeindex).echoareas.contains(echoarea)) {
            return nodeindex;
        } else {
            int node = 0;
            for (Station station : Config.values.stations) {
                if (station.echoareas.contains(echoarea)) {
                    return node;
                }
                node += 1;
            }

            return nodeindex;
        }
    }

    public static boolean delete_xc_from_station(Context context, Station station) {
        write_internal_file(context, "xc_" + station.outbox_storage_id, "");
        write_internal_file(context, "xc_tmp_" + station.outbox_storage_id, "");

        return true;
    }

    public static boolean checkTorRunning(Context context, boolean notifyUser) {
        if (Config.values.useProxy
                && Config.values.useTor
                && OrbotHelper.isOrbotInstalled(context)
                && !OrbotHelper.isOrbotRunning(context)) {
            if (!notifyUser) {
                OrbotHelper.requestStartTor(context);
                return true;
            } else return false;
        } else return true;
    }

    public static int colorFromTheme(Context callingActivity, int id) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = callingActivity.obtainStyledAttributes(typedValue.data, new int[]{id});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    public static void debug(String message) {
        Log.d(appName, message);

        if (!debugTaskFinished) {
            debugMessages.add(message);
        }
    }

    public static void pretty_debug(String message) {
        Log.d(appName, message);

        if (!debugTaskFinished) {
            prettyDebugMessages.add(message);
        }
    }

    public static void setActivityTitle(AppCompatActivity activity, String title) {
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) ab.setTitle(title);
        else SimpleFunctions.debug("Can't set activity title: " + title);
    }

    public static void setDisplayHomeAsUpEnabled(AppCompatActivity activity) {
        setDisplayHomeAsUpEnabled(activity, true);
    }

    public static void setDisplayHomeAsUpEnabled(AppCompatActivity activity, boolean state) {
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(state);
        else SimpleFunctions.debug("Can't set displayHomeAsUpEnabled");
    }

    public static boolean isTablet(Activity activity) {
        return activity.getResources().getBoolean(R.bool.isTablet);
    }
}