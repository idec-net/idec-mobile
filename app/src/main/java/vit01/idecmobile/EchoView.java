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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.EchoReadingPosition;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;

public class EchoView extends AppCompatActivity {
    String echoarea;
    ArrayList<String> msglist;
    int countMessages;
    AbstractTransport transport;
    int nodeIndex;

    RecyclerView recyclerView;
    RecyclerView.Adapter mAdapter = null;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echo_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EchoView.this, DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", echoarea);
                intent.putExtra("nodeindex", nodeIndex);
                startActivity(intent);
            }
        });

        IconicsDrawable create_icon = new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_create).color(Color.WHITE).sizeDp(20);
        fab.setImageDrawable(create_icon);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.msglist_view);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        Intent intent = getIntent();
        echoarea = intent.getStringExtra("echoarea");
        nodeIndex = intent.getIntExtra("nodeindex", -1);

        if (nodeIndex < 0) {
            fab.setVisibility(View.INVISIBLE);
        }

        transport = GlobalTransport.transport;
        loadContent(false);
    }

    boolean loadContent(boolean unread_only) {
        if (echoarea.equals("_favorites")) {
            getSupportActionBar().setTitle("Избранные");
            msglist = (unread_only) ? transport.getUnreadFavorites() : transport.getFavorites();
            countMessages = msglist.size();
        } else if (echoarea.equals("_carbon_classic")) {
            getSupportActionBar().setTitle("Карбонка");

            List<String> carbon_users = Arrays.asList(Config.values.carbon_to.split(":"));
            msglist = transport.messagesToUsers(carbon_users, Config.values.carbon_limit, unread_only);
            countMessages = msglist.size();
        } else {
            getSupportActionBar().setTitle(echoarea);

            if (unread_only) {
                msglist = transport.getUnreadMessages(echoarea);
                countMessages = msglist.size();
            } else {
                countMessages = transport.countMessages(echoarea);

                msglist = (countMessages > 0) ?
                        transport.getMsgList(echoarea, 0, 0) : SimpleFunctions.emptyList;
            }
        }

        if (countMessages == 0) {
            if (mAdapter == null) {
                // если мы только зашли в эху
                TextView this_is_empty = new TextView(this);
                this_is_empty.setText("Здесь пусто!");
                this_is_empty.setTextSize(20);
                this_is_empty.setPadding(10, 10, 10, 10);
                RelativeLayout currentLayout = (RelativeLayout)
                        findViewById(R.id.msglist_view_layout);

                currentLayout.addView(this_is_empty, 0);
            } else {
                Toast.makeText(EchoView.this, "Таких сообщений нет!", Toast.LENGTH_SHORT).show();
            }
            // возвращение false приведёт к невозможности сменить чекбокс на противоположный
            return false;
        } else {
            Collections.reverse(msglist);

            mAdapter = new MyAdapter(this, recyclerView, msglist, transport, echoarea, nodeIndex, unread_only);
            recyclerView.setAdapter(mAdapter);

            if (Config.values.disableMsglist &&
                    !echoarea.equals("_carbon_classic") && !echoarea.equals("_favorites")) {
                Intent readNow = new Intent(EchoView.this, MessageSlideActivity.class);
                ArrayList<String> normalMsgList = new ArrayList<>(msglist);
                Collections.reverse(normalMsgList);
                readNow.putExtra("msglist", normalMsgList);
                readNow.putExtra("nodeindex", nodeIndex);
                readNow.putExtra("echoarea", echoarea);

                int gotPosition = EchoReadingPosition.getPosition(echoarea);
                if (gotPosition < 0)
                    gotPosition = 0; // исправить эту строку, если при первом заходе
                // в эху хочется читать не первое сообщение, а какое-то другое

                if (gotPosition > 0 && gotPosition > (normalMsgList.size() - 1))
                    gotPosition = normalMsgList.size() - 1;
                // это предотвратит клиент от падения, если произошла чистка по ЧС или уменьшение количество мессаг в эхе

                readNow.putExtra("position", gotPosition);
                startActivity(readNow);
            }
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_echoview, menu);

        menu.findItem(R.id.action_display_unread_only).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        boolean display_unread_only = !item.isChecked();

                        if (loadContent(display_unread_only)) {
                            item.setChecked(display_unread_only);
                        }
                        return false;
                    }
                }
        );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_mark_all_read) {
            if (echoarea.equals("_carbon_classic")) {
                transport.setUnread(false, msglist);
            } else {
                transport.setUnread(false, echoarea);
            }

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        AbstractTransport transport;
        Activity callingActivity;
        String echoarea = "no.echo";
        int total_count;
        int visibleItems = 20;
        int lastVisibleItem;
        int nodeIndex;
        boolean loading, unread_only;
        int primaryColor, secondaryColor;
        private ArrayList<String> msglist;
        private ArrayList<String> visible_msglist;
        private Handler handler;
        private Drawable starredDrawable, unstarredDrawable;

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(Activity activity,
                         RecyclerView recyclerView,
                         ArrayList<String> hashes,
                         AbstractTransport db,
                         String echo,
                         int nodeindex,
                         boolean _unread_only) {
            msglist = hashes;
            transport = db;
            nodeIndex = nodeindex;
            echoarea = echo;
            callingActivity = activity;
            unread_only = _unread_only;

            total_count = msglist.size();

            if (total_count < visibleItems) {
                visible_msglist = new ArrayList<>(msglist);
            } else {
                visible_msglist = new ArrayList<>(msglist.subList(0, visibleItems));
            }

            handler = new Handler();

            final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    recyclerView.onScrolled(dx, dy);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
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
                    }, 10);
                }
            });

            int accentColor = SimpleFunctions.colorFromTheme(callingActivity, R.attr.colorAccent);
            secondaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorSecondary);
            primaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorPrimary);

            starredDrawable = new IconicsDrawable(activity)
                    .icon(GoogleMaterial.Icon.gmd_star)
                    .color(accentColor)
                    .sizeDp(20);
            unstarredDrawable = new IconicsDrawable(activity)
                    .icon(GoogleMaterial.Icon.gmd_star_border)
                    .color(secondaryColor)
                    .sizeDp(20);
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_element, parent, false);
            // set the view's size, margins, paddings and layout parameters

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.msg_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<String> normalMsglist = new ArrayList<>(msglist);
                    Collections.reverse(normalMsglist);

                    Intent intent = new Intent(callingActivity, MessageSlideActivity.class);
                    intent.putExtra("msglist", normalMsglist);

                    if (!echoarea.equals("_carbon_classic") && !echoarea.equals("_favorites") && !unread_only) {
                        intent.putExtra("echoarea", echoarea);
                        intent.putExtra("nodeindex", nodeIndex);
                    }
                    int pos = total_count - holder.position - 1;
                    intent.putExtra("position", pos);
                    callingActivity.startActivity(intent);
                }
            });

            final ImageView star = (ImageView) v.findViewById(R.id.msg_star);
            star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    IIMessage message = transport.getMessage(holder.msgid);
                    message.is_favorite = !message.is_favorite;

                    if (message.is_favorite) {
                        star.setImageDrawable(starredDrawable);
                    } else {
                        star.setImageDrawable(unstarredDrawable);
                    }

                    transport.setFavorite(message.is_favorite, Collections.singletonList(holder.msgid));
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
            holder.msg_text.setText(SimpleFunctions.messagePreview(message.msg));
            holder.msg_date.setText(DateUtils.getRelativeTimeSpanString(message.time * 1000));

            if (message.is_favorite) {
                holder.msg_star.setImageDrawable(starredDrawable);
            } else {
                holder.msg_star.setImageDrawable(unstarredDrawable);
            }

            int font_style = (message.is_unread) ? Typeface.BOLD : Typeface.NORMAL;

            if (message.is_unread) {
                holder.msg_text.setTextColor(primaryColor);
            } else {
                holder.msg_text.setTextColor(secondaryColor);
            }

            holder.msg_subj.setTypeface(null, font_style);
            holder.msg_from_to.setTypeface(null, font_style);
            holder.msg_text.setTypeface(null, font_style);
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
            public ImageView msg_star;
            public String msgid;
            public int position;

            public ViewHolder(View myLayout) {
                super(myLayout);
                msg_subj = (TextView) myLayout.findViewById(R.id.msg_subj);
                msg_from_to = (TextView) myLayout.findViewById(R.id.msg_from_to);
                msg_text = (TextView) myLayout.findViewById(R.id.msg_text);
                msg_date = (TextView) myLayout.findViewById(R.id.msg_date);
                msg_star = (ImageView) myLayout.findViewById(R.id.msg_star);
            }
        }
    }
}