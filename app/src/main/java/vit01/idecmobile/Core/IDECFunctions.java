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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import vit01.idecmobile.Strings;
import vit01.idecmobile.prefs.Config;

public class IDECFunctions {
    public static ArrayList<String> loadAreaMessages(String echoarea, boolean unread_only) {
        AbstractTransport transport = GlobalTransport.transport();
        ArrayList<String> msglist;
        if (echoarea == null) return SimpleFunctions.emptyList;
        String sort = Config.values.sortByDate ? "date" : "number";

        switch (echoarea) {
            case "_favorites":
                msglist = (unread_only) ? transport.getUnreadFavorites(sort) : transport.getFavorites(sort);
                break;
            case "_carbon_classic":
                List<String> carbon_users = Arrays.asList(Config.values.carbon_to.split(":"));

                msglist = transport.messagesToUsers(carbon_users, Config.values.carbon_limit, unread_only, sort);
                break;
            case "_unread":
                msglist = transport.getAllUnreadMessages(sort);
                break;
            default:
                if (unread_only) {
                    msglist = transport.getUnreadMessages(echoarea, sort);
                } else {
                    msglist = transport.getMsgList(echoarea, 0, 0, sort);
                }
                break;
        }

        return (msglist != null) ? msglist : SimpleFunctions.emptyList;
    }

    public static String getAreaName(String echoarea) {
        if (echoarea == null || echoarea.equals("") || echoarea.equals("no.echo")) {
            return "";
        }

        switch (echoarea) {
            case "_favorites":
                return Strings.favorites;
            case "_carbon_classic":
                return Strings.carbon;
            case "_unread":
                return Strings.unread;
            case "_search_results":
                return Strings.search_results;
            default:
                return echoarea;
        }
    }

    public static boolean isRealEchoarea(String echoarea) {
        if (echoarea == null) return false;

        switch (echoarea) {
            case "_favorites":
                return false;
            case "_carbon_classic":
                return false;
            case "_unread":
                return false;
            case "_search_results":
                return false;
            case "no.echo":
                return false;
            case "null":
                return false;
            case "":
                return false;
            default:
                return true;
        }
    }

    public static int getNodeIndex(String echoarea, boolean allow_false_results) {
        int nodeindex = -1;

        int i = 0;
        for (Station station : Config.values.stations) {
            if (station.echoareas.contains(echoarea)) {
                nodeindex = i;
                break;
            }
            i += 1;
        }

        if (nodeindex == -1 && !allow_false_results) {
            nodeindex = Config.currentSelectedStation;
        }

        return nodeindex;
    }

    public static ArrayList<String> getStationsNames() {
        ArrayList<String> stationNames = new ArrayList<>();
        for (Station station : Config.values.stations) {
            stationNames.add(station.nodename);
        }

        return stationNames;
    }
}