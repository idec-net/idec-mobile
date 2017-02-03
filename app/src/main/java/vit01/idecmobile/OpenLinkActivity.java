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

package vit01.idecmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SqliteTransport;
import vit01.idecmobile.Core.Station;

public class OpenLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String ii_link = getIntent().getData().getHost();

        if (ii_link.contains(".")) {
            // значит это эха!
            Intent intent = new Intent(OpenLinkActivity.this, EchoView.class);
            int nodeindex = -1;

            int i = 0;
            for (Station station : Config.values.stations) {
                if (station.echoareas.contains(ii_link)) {
                    nodeindex = i;
                    break;
                }
                i += 1;
            }

            if (nodeindex == -1) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                nodeindex = sharedPref.getInt("nodeindex_current", 0);
            }

            intent.putExtra("echoarea", ii_link);
            intent.putExtra("nodeindex", nodeindex);
            startActivity(intent);
        } else {
            // иначе сообщение
            Intent intent = new Intent(OpenLinkActivity.this, MessageSlideActivity.class);
            AbstractTransport transport = new SqliteTransport(this);

            IIMessage message = transport.getMessage(ii_link);
            if (message == null) message = new IIMessage();
            ArrayList<String> msglist = transport.getMsgList(message.echo, 0, 0);

            if (msglist.contains(ii_link)) {
                int position = msglist.lastIndexOf(ii_link);

                intent.putExtra("msglist", msglist);
                intent.putExtra("position", position);
            } else {
                msglist = new ArrayList<>();
                msglist.add(ii_link);

                intent.putExtra("msglist", msglist);
                intent.putExtra("position", 0);
            }
            startActivity(intent);
        }
        finish();
    }
}
