package vit01.idecmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;

public class EchoView extends AppCompatActivity {
    String echoarea;
    int countMessages;
    AbstractTransport transport;

    RecyclerView recyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

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
        getSupportActionBar().setTitle(echoarea);
        int nodeIndex = intent.getIntExtra("nodeindex", -1);

        if (nodeIndex < 0) {
            fab.setVisibility(View.INVISIBLE);
        }

        recyclerView = (RecyclerView) findViewById(R.id.msglist_view);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);


        transport = new SqliteTransport(getApplicationContext());
        countMessages = transport.countMessages(echoarea);

        if (countMessages == 0) {
            TextView this_is_empty = new TextView(this);
            this_is_empty.setText("Эха пуста!");
            this_is_empty.setTextSize(20);
            this_is_empty.setPadding(10, 10, 10, 10);
            RelativeLayout currentLayout = (RelativeLayout)
                    findViewById(R.id.msglist_view_layout);

            currentLayout.addView(this_is_empty, 0);
        } else {
            ArrayList<String> msglist;
            msglist = transport.getMsgList(echoarea, 0, 0);

            Collections.reverse(msglist);

            mAdapter = new MyAdapter(this, recyclerView, msglist, transport);
            recyclerView.setAdapter(mAdapter);
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        AbstractTransport transport;
        Activity callingActivity;
        int total_count;
        int visibleItems = 5;
        int lastVisibleItem;
        boolean loading;
        private ArrayList<String> msglist;
        private ArrayList<String> visible_msglist;

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(Activity activity,
                         RecyclerView recyclerView,
                         ArrayList<String> hashes,
                         AbstractTransport db) {
            msglist = hashes;
            transport = db;
            callingActivity = activity;
            visible_msglist = new ArrayList<>(msglist.subList(0, visibleItems));
            total_count = msglist.size();

            final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    recyclerView.onScrolled(dx, dy);
                    int countitems = layoutManager.getItemCount();
                    lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                    if (!loading && lastVisibleItem == countitems - 1 && countitems < total_count) {
                        loading = true;
                        for (int i = 1; i <= visibleItems; i++) {
                            int itemToInsert = lastVisibleItem + i;

                            if (itemToInsert == total_count) break;

                            visible_msglist.add(msglist.get(itemToInsert));
                            notifyItemInserted(itemToInsert);
                        }
                        loading = false;
                    }
                }
            });
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_element, parent, false);
            // set the view's size, margins, paddings and layout parameters

            LinearLayout l = (LinearLayout) v.findViewById(R.id.msg_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<String> normalMsglist = new ArrayList<>(msglist);
                    Collections.reverse(normalMsglist);

                    Intent intent = new Intent(callingActivity, MessageSlideActivity.class);
                    intent.putExtra("msglist", normalMsglist);
                    int pos = total_count - holder.position - 1;
                    intent.putExtra("position", pos);
                    callingActivity.startActivity(intent);
                }
            });

            return holder;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.msgid = visible_msglist.get(position);
            holder.position = position;
            IIMessage message = transport.getMessage(holder.msgid);
            if (message == null) message = new IIMessage();

            holder.msg_subj.setText(message.subj);
            holder.msg_from_to.setText(message.from + " to " + message.to);
            holder.msg_text.setText(message.msg);
            holder.msg_date.setText(SimpleFunctions.timestamp2date(message.time, false));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return visible_msglist.size();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView msg_subj, msg_from_to, msg_text, msg_date;
            public CheckBox msg_favorite;
            public String msgid;
            public int position;

            public ViewHolder(View myLayout) {
                super(myLayout);
                msg_subj = (TextView) myLayout.findViewById(R.id.msg_subj);
                msg_from_to = (TextView) myLayout.findViewById(R.id.msg_from_to);
                msg_text = (TextView) myLayout.findViewById(R.id.msg_text);
                msg_date = (TextView) myLayout.findViewById(R.id.msg_date);
            }
        }
    }
}