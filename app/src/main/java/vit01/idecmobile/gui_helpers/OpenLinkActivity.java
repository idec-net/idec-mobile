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

package vit01.idecmobile.gui_helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.GUI.Reading.EchoReaderActivity;

public class OpenLinkActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri ii_link_data = getIntent().getData();
        if (ii_link_data == null) {
            finish();
            return;
        }
        String ii_link = ii_link_data.getHost();

        if (ii_link.contains(".")) {
            // значит это эха!
            Intent intent = new Intent(OpenLinkActivity.this, EchoReaderActivity.class);

            intent.putExtra("echoarea", ii_link);
            intent.putExtra("nodeindex", IDECFunctions.getNodeIndex(ii_link, false));
            startActivity(intent);
        } else {
            // иначе сообщение
            Intent intent = new Intent(OpenLinkActivity.this, EchoReaderActivity.class);
            AbstractTransport transport = GlobalTransport.transport();

            IIMessage message = transport.getMessage(ii_link);
            if (message == null) {
                ArrayList<String> trySearch = transport.searchSimilarMsgids(ii_link);
                if (trySearch == null || trySearch.size() == 0) {
                    message = new IIMessage();
                    message.is_corrupt = true;
                } else {
                    ii_link = trySearch.get(0);
                    message = transport.getMessage(ii_link);
                }
            }

            // Если сообщения в базе нет, то выдаётся "ненастоящая" эха no.echo.
            // Иначе имеет смысл загрузить эху и показать сообщение внутри неё

            boolean isRealEcho = IDECFunctions.isRealEchoarea(message.echo);
            int nodeindex = -1;

            if (isRealEcho) {
                intent.putExtra("echoarea", message.echo);
                nodeindex = IDECFunctions.getNodeIndex(message.echo, true);
            } else {
                ArrayList<String> s = new ArrayList<>();
                s.add(ii_link);

                intent.putExtra("echoarea", "no.echo");
                intent.putStringArrayListExtra("msglist", s);
            }
            intent.putExtra("msgid", ii_link);
            intent.putExtra("nodeindex", nodeindex);
            startActivity(intent);
        }
        finish();
    }
}
