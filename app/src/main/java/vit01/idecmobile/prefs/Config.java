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

package vit01.idecmobile.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import vit01.idecmobile.Core.GlobalConfig;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.R;

public class Config {
    public static GlobalConfig values;
    public static GlobalConfig default_values;
    public static String filename = "config.obj";
    public static int appTheme = R.style.AppTheme;
    public static int currentSelectedStation;
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor prefEditor;
    public static Context lastContext = null;
    public static boolean isKDEConnectInstalled = false;

    public static void loadConfig(Context context, String filename) {
        lastContext = context;
        default_values = new GlobalConfig();

        try {
            FileInputStream is = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(is);
            values = (GlobalConfig) ois.readObject();
            ois.close();
            is.close();
            configUpdate(context);
        } catch (Exception e) {
            SimpleFunctions.debug("Config is not found (or error), create from default");
            e.printStackTrace();

            values = new GlobalConfig();
        }
        select_gui_theme();
        isKDEConnectInstalled = appInstalledOrNot(context, "org.kde.kdeconnect_tp");
    }

    public static void loadConfig(Context context) {
        loadConfig(context, filename);
    }

    public static void writeConfig(Context context) {
        if (context == null) context = lastContext;

        try {
            FileOutputStream os = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(values);
            oos.close();
            os.close();
        } catch (Exception e) {
            SimpleFunctions.debug("Error writing config");
            e.printStackTrace();
        }
    }

    public static void configUpdate(Context context) {
        boolean needForWrite = false;

        if (Config.values.carbon_to == null) {
            Config.values.carbon_to = "All";
            needForWrite = true;
        }

        if (Config.values.carbon_limit <= 0) {
            Config.values.carbon_limit = 50;
            needForWrite = true;
        }

        if (Config.values.notifyFireDuration <= 0) {
            Config.values.notifyFireDuration = 15;
            needForWrite = true;
        }

        if (Config.values.proxyAddress == null) {
            Config.values.proxyAddress = "127.0.0.1:8118";
        }

        if (Config.values.applicationTheme == null) {
            Config.values.applicationTheme = "default";
            needForWrite = true;
        }

        for (Station station : Config.values.stations) {
            if (station.outbox_storage_id == null) {
                station.outbox_storage_id = SimpleFunctions.getRandomUUID();
                needForWrite = true;
            }
        }

        for (Station station : Config.values.stations) {
            if (station.file_echoareas == null) {
                station.file_echoareas = new ArrayList<>(Config.default_values.stations.get(0).file_echoareas);
                needForWrite = true;
            }
        }

        if (Config.values.textsignature == null) {
            Config.values.textsignature = "";
            needForWrite = true;
        }

        if (needForWrite) writeConfig(context);
    }

    public static void select_gui_theme() {
        switch (Config.values.applicationTheme) {
            case "dark":
                appTheme = R.style.AppTheme_Dark;
                break;

            case "white":
                appTheme = R.style.AppTheme_TheWhite;
                break;

            case "white_pink":
                appTheme = R.style.AppTheme_TheWhite_Pink;
                break;

            case "tomorrow_night_eighties":
                appTheme = R.style.AppTheme_TomorrowNightEighties;
                break;

            case "orangehub":
                appTheme = R.style.AppTheme_IDECHub;
                break;

            case "default":
            default:
                appTheme = R.style.AppTheme;
                break;
        }
    }

    public static int getCurrentStationPosition(Context context) {
        if (sharedPref == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        }

        currentSelectedStation = sharedPref.getInt("nodeindex_current", 0);
        return currentSelectedStation;
    }

    public static void saveCurrentStationPosition() {
        prefEditor = sharedPref.edit();
        prefEditor.putInt("nodeindex_current", currentSelectedStation);
        prefEditor.apply();
    }

    private static boolean appInstalledOrNot(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}