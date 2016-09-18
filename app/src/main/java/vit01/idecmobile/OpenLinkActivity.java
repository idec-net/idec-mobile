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
