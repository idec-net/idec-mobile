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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import java.util.Collections;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.EchoReadingPosition;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.gui_helpers.DividerItemDecoration;
import vit01.idecmobile.prefs.Config;

public class MessageListFragment extends Fragment {
    String echoarea;
    ArrayList<String> msglist;
    int countMessages;
    int nodeIndex;

    RecyclerView recyclerView;
    RecyclerView.Adapter mAdapter = null;
    RecyclerView.LayoutManager mLayoutManager;

    public MessageListFragment() {
    }

    public static MessageListFragment newInstance(

    ) {
        return new MessageListFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* if (getArguments() != null) {
            // echoareas = getArguments().getStringArrayList("echolist");
            // nodeindex = getArguments().getInt("nodeindex");
        } else echoareas = new ArrayList<>();
        */
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_msglist, container, false);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", echoarea);
                intent.putExtra("nodeindex", nodeIndex);
                startActivity(intent);
                Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
            }
        });

        IconicsDrawable create_icon = new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_create).color(Color.WHITE).sizeDp(19);
        fab.setImageDrawable(create_icon);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.msglist_view);
        mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext()));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void initEchoView(String echo, ArrayList<String> msgids, int nIndex) {
        echoarea = echo;
        msglist = msgids;
        nodeIndex = nIndex;

        loadContent(false);
    }

    public void showEmptyView() {
        // Отображаем окошечко, что здесь пусто
        View view = getActivity().getLayoutInflater().inflate(R.layout.content_empty, null);
        RelativeLayout l = (RelativeLayout) view.findViewById(R.id.content_empty_layout);
        ((CoordinatorLayout) view.getRootView()).removeAllViews();

        RelativeLayout current = (RelativeLayout) getActivity().findViewById(R.id.msglist_view_layout);
        current.removeAllViews();
        current.addView(l);
    }

    boolean loadContent(boolean unread_only) {
        if (msglist == null) {
            msglist = IDECFunctions.loadAreaMessages(echoarea, unread_only);
        }
        countMessages = msglist.size();

        Activity activity = getActivity();

        if (countMessages == 0) {
            if (mAdapter == null) {
                showEmptyView();
            } else {
                Toast.makeText(activity, "Таких сообщений нет!", Toast.LENGTH_SHORT).show();
            }
            // возвращение false приведёт к невозможности сменить чекбокс на противоположный
            return false;
        } else {
            Collections.reverse(msglist);

            mAdapter = new MyAdapter(activity, recyclerView, msglist, GlobalTransport.transport, echoarea, nodeIndex, unread_only);
            recyclerView.setAdapter(mAdapter);

            boolean isTablet = SimpleFunctions.isTablet(activity);
            int gotPosition = 0;

            ArrayList<String> normalMsgList = new ArrayList<>(msglist);
            Collections.reverse(normalMsgList);

            if ((Config.values.disableMsglist || isTablet) &&
                    !echoarea.equals("_carbon_classic") &&
                    !echoarea.equals("_favorites")) {

                if (!echoarea.equals("_unread")) {
                    String lastMsgid = EchoReadingPosition.getPosition(echoarea);

                    if (lastMsgid != null && normalMsgList.contains(lastMsgid)) {
                        gotPosition = normalMsgList.lastIndexOf(lastMsgid);
                    }
                }

                if (gotPosition < 0)
                    gotPosition = 0; // исправить эту строку, если при первом заходе
                // в эху хочется читать не первое сообщение, а какое-то другое

                if (gotPosition > 0 && gotPosition > (normalMsgList.size() - 1))
                    gotPosition = normalMsgList.size() - 1;
                // это предотвратит клиент от падения, если произошла чистка по ЧС или уменьшение количество мессаг в эхе

                if (!isTablet && !unread_only) {
                    Intent readNow = new Intent(activity, MessageSlideActivity.class);
                    readNow.putExtra("msglist", normalMsgList);
                    readNow.putExtra("nodeindex", nodeIndex);
                    readNow.putExtra("echoarea", echoarea);
                    readNow.putExtra("position", gotPosition);
                    startActivityForResult(readNow, 1);
                }
            }

            if (!IDECFunctions.isRealEchoarea(echoarea)) {
                gotPosition = msglist.size() - 1;
            }

            if (isTablet) {
                ((MessageSlideFragment) getFragmentManager().findFragmentById(R.id.messages_slider))
                        .initSlider(echoarea, normalMsgList, nodeIndex, gotPosition);
            }
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Activity activity = getActivity();

        int iconColor = SimpleFunctions.colorFromTheme(activity, R.attr.menuIconColor);
        if (echoarea.equals("_favorites")) {
            MenuItem favItem = menu.findItem(R.id.action_favorites_remove_all);
            favItem.setVisible(true);
            favItem.setIcon(new IconicsDrawable(activity,
                    GoogleMaterial.Icon.gmd_clear_all).actionBar()
                    .color(iconColor));
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Activity activity = getActivity();
        int id = item.getItemId();

        switch (id) {
            case R.id.action_mark_all_read:
                if (echoarea.equals("_carbon_classic")) {
                    GlobalTransport.transport.setUnread(false, msglist);
                } else {
                    GlobalTransport.transport.setUnread(false, echoarea);
                }

                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.action_display_unread_only:
                boolean display_unread_only = !item.isChecked();

                if (loadContent(display_unread_only)) {
                    item.setChecked(display_unread_only);
                }
                break;
            case R.id.action_favorites_remove_all:
                new AlertDialog.Builder(activity)
                        .setTitle("Очистить избранные")
                        .setMessage("Снять со всех сообщений данную метку?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final ProgressDialog progress = ProgressDialog.show(activity, "Подождите", "Сообщения удаляются...", true);
                                progress.show();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ArrayList<String> favorites = GlobalTransport.transport.getFavorites();
                                        GlobalTransport.transport.setFavorite(false, favorites);
                                        progress.dismiss();
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(activity, "Выполнено!", Toast.LENGTH_SHORT).show();
                                                activity.finish();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity,
                                        "Правильно, пусть останутся!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
                break;
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
        boolean loading, unread_only, isTablet;
        int primaryColor, secondaryColor;
        private ArrayList<String> msglist;
        private ArrayList<String> visible_msglist;
        private Handler handler;
        private Drawable starredDrawable, unstarredDrawable;

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
            isTablet = SimpleFunctions.isTablet(callingActivity);

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

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_element, parent, false);

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.msg_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = total_count - holder.position - 1;

                    if (!isTablet) {
                        ArrayList<String> normalMsglist = new ArrayList<>(msglist);
                        Collections.reverse(normalMsglist);

                        Intent intent = new Intent(callingActivity, MessageSlideActivity.class);
                        intent.putExtra("msglist", normalMsglist);

                        if (IDECFunctions.isRealEchoarea(echoarea) && !unread_only) {
                            intent.putExtra("echoarea", echoarea);
                            intent.putExtra("nodeindex", nodeIndex);
                        }
                        intent.putExtra("position", pos);
                        callingActivity.startActivity(intent);
                    } else {
                        ((MessageSlideFragment) ((AppCompatActivity) callingActivity)
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.messages_slider))
                                .mPager.setCurrentItem(pos);
                    }
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

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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

        @Override
        public int getItemCount() {
            return visible_msglist.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
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
