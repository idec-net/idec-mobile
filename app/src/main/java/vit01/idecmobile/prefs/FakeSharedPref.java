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

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import vit01.idecmobile.Core.SimpleFunctions;

public class FakeSharedPref implements SharedPreferences {
    private SharedPreferences.OnSharedPreferenceChangeListener listener = null;
    private Context context;

    public FakeSharedPref(Context configContext) {
        context = configContext;
    }

    @Nullable
    @Override
    public String getString(String s, String s1) {
        switch (s) {
            case "carbon_to":
                return Config.values.carbon_to;
            case "proxy_address":
                return Config.values.proxyAddress;
            case "application_theme":
                return Config.values.applicationTheme;
            case "textsignature":
                return Config.values.textsignature;
        }
        return s1;
    }

    @Override
    public int getInt(String s, int i) {
        switch (s) {
            case "one_request_limit":
                return Config.values.oneRequestLimit;
            case "connection_timeout":
                return Config.values.connectionTimeout;
            case "carbon_limit":
                return Config.values.carbon_limit;
            case "notify_fire_duration":
                return Config.values.notifyFireDuration;
            case "proxy_type":
                return Config.values.proxyType;
        }
        return i;
    }

    @Override
    public boolean getBoolean(String s, boolean b) {
        switch (s) {
            case "default_editor":
                return Config.values.defaultEditor;
            case "first_run":
                return Config.values.firstRun;
            case "use_proxy":
                return Config.values.useProxy;
            case "use_tor":
                return Config.values.useTor;
            case "old_quote":
                return Config.values.oldQuote;
            case "notifications_enabled":
                return Config.values.notificationsEnabled;
            case "notifications_vibrate":
                return Config.values.notificationsVibrate;
            case "autofetch_enabled":
                return Config.values.autoFetchEnabled;
            case "swipe_to_fetch":
                return Config.values.swipeToFetch;
            case "hide_toolbar_when_scrolling":
                return Config.values.hide_toolbar_when_scrolling;
            case "disable_msglist":
                return Config.values.disableMsglist;
            case "sort_by_date":
                return Config.values.sortByDate;
            case "open_unread_after_fetch":
                return Config.values.openUnreadAfterFetch;
        }
        return b;
    }

    @Override
    public Editor edit() {
        return new SharedPrefEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        listener = onSharedPreferenceChangeListener;
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        listener = null;
    }

    @Override
    public boolean contains(String s) {
        return true;
    }

    @Override
    public long getLong(String s, long l) {
        return 0;
    }

    @Override
    public float getFloat(String s, float v) {
        return 0;
    }

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>();
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String s, Set<String> set) {
        return new HashSet<>();
    }


    private class SharedPrefEditor implements SharedPreferences.Editor {
        @Override
        public SharedPreferences.Editor putString(String s, String s1) {
            switch (s) {
                case "carbon_to":
                    Config.values.carbon_to = s1;
                    break;
                case "proxy_address":
                    Config.values.proxyAddress = s1;
                    break;
                case "application_theme":
                    Config.values.applicationTheme = s1;
                    break;
                case "textsignature":
                    Config.values.textsignature = s1;
                    break;
            }
            if (listener != null) listener.onSharedPreferenceChanged(FakeSharedPref.this, s);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String s, int i) {
            switch (s) {
                case "one_request_limit":
                    Config.values.oneRequestLimit = i;
                    break;
                case "connection_timeout":
                    Config.values.connectionTimeout = i;
                    break;
                case "carbon_limit":
                    Config.values.carbon_limit = i;
                    break;
                case "notify_fire_duration":
                    Config.values.notifyFireDuration = i;
                    break;
                case "proxy_type":
                    Config.values.proxyType = i;
                    break;
            }

            if (listener != null) listener.onSharedPreferenceChanged(FakeSharedPref.this, s);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String s, boolean b) {
            switch (s) {
                case "default_editor":
                    Config.values.defaultEditor = b;
                    break;
                case "first_run":
                    Config.values.firstRun = b;
                    break;
                case "use_proxy":
                    Config.values.useProxy = b;
                    break;
                case "use_tor":
                    Config.values.useTor = b;
                    break;
                case "old_quote":
                    Config.values.oldQuote = b;
                    break;
                case "notifications_enabled":
                    Config.values.notificationsEnabled = b;
                    break;
                case "notifications_vibrate":
                    Config.values.notificationsVibrate = b;
                    break;
                case "autofetch_enabled":
                    Config.values.autoFetchEnabled = b;
                    break;
                case "swipe_to_fetch":
                    Config.values.swipeToFetch = b;
                    break;
                case "hide_toolbar_when_scrolling":
                    Config.values.hide_toolbar_when_scrolling = b;
                    break;
                case "disable_msglist":
                    Config.values.disableMsglist = b;
                    break;
                case "sort_by_date":
                    Config.values.sortByDate = b;
                    break;
                case "open_unread_after_fetch":
                    Config.values.openUnreadAfterFetch = b;
                    break;
            }

            if (listener != null) listener.onSharedPreferenceChanged(FakeSharedPref.this, s);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String s, long l) {
            return null;
        }

        @Override
        public SharedPreferences.Editor putFloat(String s, float v) {
            return null;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String s, Set<String> set) {
            return null;
        }

        @Override
        public SharedPreferences.Editor remove(String s) {
            return null;
        }

        @Override
        public SharedPreferences.Editor clear() {
            return null;
        }

        @Override
        public boolean commit() {
            SimpleFunctions.debug("Write config from preferences...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Config.writeConfig(context);
                }
            }).start();

            return true;
        }

        // Этот метод нихрена не делает, это так и надо!
        @Override
        public void apply() {}
    }
}