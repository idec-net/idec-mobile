package vit01.idecmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class EchoView extends AppCompatActivity {
    String echoarea;
    int countMessages;
    int groupN = 10;
    AbstractTransport transport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echo_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Написание новых сообщений пока не работает. Всему своё время!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        echoarea = intent.getStringExtra("echoarea");
        int nodeIndex = intent.getIntExtra("nodeindex", -1);

        if (nodeIndex < 0) {
            fab.setVisibility(View.INVISIBLE);
        }

        transport = new SqliteTransport(getApplicationContext());
        countMessages = transport.countMessages(echoarea);

        if (countMessages == 0) {
            TextView this_is_empty = new TextView(this);
            this_is_empty.setText("Эха пуста!");
            this_is_empty.setTextSize(20);
            RelativeLayout currentLayout = (RelativeLayout)
                    findViewById(R.id.msglist_view_layout);

            currentLayout.addView(this_is_empty, 0);
        } else {
            ArrayList<String> msglist;
            ArrayList<IIMessage> newMessages = new ArrayList<>();

            if (countMessages < groupN) {
                msglist = transport.getMsgList(echoarea, 0, 0);
            } else {
                msglist = transport.getMsgList(echoarea, countMessages - groupN, groupN);
            }

            Collections.reverse(msglist);

            Hashtable<String, IIMessage> messages = transport.getMessages(msglist);

            for (String msgid : msglist) {
                newMessages.add(messages.get(msgid));
            }

            ListView v = (ListView) findViewById(R.id.msglist_view);
            ListAdapter adapter = new ArrayAdapter<IIMessage>(this, android.R.layout.activity_list_item, newMessages);
            v.setAdapter(adapter);
        }
    }
}