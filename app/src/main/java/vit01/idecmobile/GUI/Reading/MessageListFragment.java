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

package vit01.idecmobile.GUI.Reading;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
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
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.EchoReadingPosition;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.GUI.Drafts.DraftEditor;
import vit01.idecmobile.R;
import vit01.idecmobile.gui_helpers.DividerItemDecoration;
import vit01.idecmobile.prefs.Config;

public class MessageListFragment extends Fragment {
    public static boolean alreadyOpenedSliderActivity = false;
    String echoarea;
    ArrayList<String> msglist;
    int countMessages;
    int nodeIndex;
    MessageSlideFragment slider = null;
    RecyclerView recyclerView;
    MessageListFragment.MyAdapter mAdapter = null;
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
        // По идее, мы здесь должны смотреть getArguments() и искать нужное, но
        // нафиг надо
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
            }
        });

        Activity activity = getActivity();
        IconicsDrawable create_icon = new IconicsDrawable(activity).icon(GoogleMaterial.Icon.gmd_create)
                .color(SimpleFunctions.colorFromTheme(activity, R.attr.fabIconColor)).sizeDp(19);
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
        slider = (MessageSlideFragment) getFragmentManager().findFragmentById(R.id.messages_slider);

        boolean lastUnreadOnly = getActivity().getIntent().getBooleanExtra("unread_only", false);
        loadContent(lastUnreadOnly);
    }

    public void showEmptyView() {
        // Отображаем окошечко, что здесь пусто
        Activity activity = getActivity();

        View view = activity.getLayoutInflater().inflate(R.layout.content_empty, null, false);
        RelativeLayout l = (RelativeLayout) view.findViewById(R.id.content_empty_layout);
        ((CoordinatorLayout) view.getRootView()).removeAllViews();

        ViewGroup current = (RelativeLayout) activity.findViewById(R.id.msglist_view_layout);

        current.removeAllViews();
        current.addView(l);

        if (SimpleFunctions.isTablet(getActivity())) {
            // На планшетах просто растягиваем на весь экран фрагмент
            // Будет правильнее не удалять всё нафиг, а именно растянуть,
            // чтобы не потерять кнопку для написания нового сообщения.

            ViewGroup fragm_layout = (LinearLayout) activity.findViewById(R.id.fragments_layout_container);
            fragm_layout.removeViewAt(2); // Удаляем правый фрагмент и разделитель
            fragm_layout.removeViewAt(1);
            current = (LinearLayout) activity.findViewById(R.id.msglist_container);
            current.setLayoutParams(new LinearLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    boolean loadContent(boolean unread_only) {
        ArrayList<String> tmp_msglist;
        Activity activity = getActivity();
        Intent currentIntent = activity.getIntent();

        String requestedMsgid = currentIntent.getStringExtra("msgid");
        if (requestedMsgid != null) currentIntent.removeExtra("msgid");
        // Зануляем msgid, потому что алгоритм всё равно его запомнит, и это позволит избежать
        // проблем с поворотом экрана и с запуском слайдер-активити

        boolean lastUnreadOnly = currentIntent.getBooleanExtra("unread_only", false);

        if (msglist == null || unread_only != lastUnreadOnly) {
            tmp_msglist = IDECFunctions.loadAreaMessages(echoarea, unread_only);
            currentIntent.putExtra("msglist", tmp_msglist);
            if (tmp_msglist.size() > 0)
                currentIntent.putExtra("unread_only", unread_only);
            activity.setIntent(currentIntent);
        } else {
            tmp_msglist = msglist;
            Collections.reverse(tmp_msglist);
        }

        int tmp_countMessages = tmp_msglist.size();

        if (tmp_countMessages == 0) {
            if (mAdapter == null) {
                msglist = tmp_msglist;
                countMessages = 0;

                if (echoarea.equals("_unread")) {
                    Toast.makeText(activity, R.string.no_unread_messages, Toast.LENGTH_SHORT).show();
                    activity.finish();
                    return false;
                }

                showEmptyView();
            } else {
                Toast.makeText(activity, R.string.no_such_messages, Toast.LENGTH_SHORT).show();
            }
            // возвращение false приведёт к невозможности сменить чекбокс
            // [только непрочитанные] на противоположный
            return false;
        }

        msglist = tmp_msglist;
        countMessages = tmp_countMessages;
        Collections.reverse(msglist);

        boolean isTablet = SimpleFunctions.isTablet(activity);
        int gotPosition = 0;
        int lastPosition = msglist.size() - 1;

        ArrayList<String> normalMsgList = new ArrayList<>(msglist);
        Collections.reverse(normalMsgList);

        switch (echoarea) {
            case "_carbon_classic":
                gotPosition = lastPosition;
                break;
            case "_favorites":
                gotPosition = lastPosition;
                break;
            case "_search_results":
                gotPosition = 0;
                break;
            case "_unread":
                gotPosition = 0;
                break;
            default:
                if (!IDECFunctions.isRealEchoarea(echoarea)) gotPosition = 0;
                else {
                    String savedPositionMsgid = EchoReadingPosition.getPosition(echoarea);
                    String lastMsgid = requestedMsgid != null ? requestedMsgid : savedPositionMsgid;

                    if (lastMsgid != null && normalMsgList.contains(lastMsgid)) {
                        gotPosition = normalMsgList.lastIndexOf(lastMsgid);
                    }
                }
                break;
        }

        if (!isTablet && !unread_only
                && !echoarea.equals("_carbon_classic")
                && !echoarea.equals("_favorites")
                && (!alreadyOpenedSliderActivity || requestedMsgid != null)
                && Config.values.disableMsglist
                ) {
            Intent readNow = new Intent(activity, MessageSlideActivity.class);
            readNow.putExtra("msglist", normalMsgList);
            readNow.putExtra("nodeindex", nodeIndex);
            readNow.putExtra("echoarea", echoarea);
            readNow.putExtra("position", gotPosition);
            alreadyOpenedSliderActivity = true;
            activity.startActivityForResult(readNow, 1);
        }

        int reversedPosition = msglist.size() - 1 - gotPosition; // мы ведь перевернули список, значит и позицию надо
        mAdapter = new MyAdapter(activity, recyclerView, msglist, GlobalTransport.transport,
                echoarea, nodeIndex, reversedPosition);
        recyclerView.setAdapter(mAdapter);

        if (isTablet) {
            alreadyOpenedSliderActivity = false;
            slider.initSlider(echoarea, normalMsgList, nodeIndex, gotPosition);
        }
        recyclerView.scrollToPosition(reversedPosition);
        mAdapter.lastSelectedItem = reversedPosition;
        mAdapter.setSelection(reversedPosition);

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

        if (echoarea.equals("_unread") || echoarea.equals("_search_results")) {
            MenuItem action_unread_only = menu.findItem(R.id.action_display_unread_only);
            action_unread_only.setVisible(false);
        }

        if (countMessages <= 1) menu.findItem(R.id.action_search).setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Activity activity = getActivity();
        int id = item.getItemId();

        switch (id) {
            case R.id.action_mark_all_read:
                if (!IDECFunctions.isRealEchoarea(echoarea)) {
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
                        .setTitle(R.string.clear_favorites)
                        .setMessage(R.string.clear_favorites_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final ProgressDialog progress = ProgressDialog.show(activity,
                                        activity.getString(R.string.wait), activity.getString(R.string.deleting_messages), true);
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
                                                Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show();
                                                activity.finish();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity, R.string.ok_let_them_exist, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
                break;
            case R.id.action_debug_export_links:
                StringBuilder links = new StringBuilder();
                String currentLink = "";
                for (String msgid: msglist) {
                    IIMessage msg = GlobalTransport.transport.getMessage(msgid);
                    Matcher lnk = Patterns.WEB_URL.matcher(msg.msg);
                    while (lnk.find()) {
                        currentLink = lnk.group();
                    }
                    if (!currentLink.equals("")) {
                        links.append(currentLink).append("\n");
                    }
                    currentLink = "";
                }

                File file = new File(ExternalStorage.rootStorage, "links.txt");
                if (!file.exists()) try {
                    boolean create = file.createNewFile();

                    if (!create) {
                        String debug = getString(R.string.create_file_error) + " " + file.getName();
                        Toast.makeText(activity, debug, Toast.LENGTH_SHORT).show();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    SimpleFunctions.debug(e.getMessage());
                    break;
                }
                if (file.canWrite()) {
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(links.toString().getBytes("UTF-8"));
                        fos.close();
                    } catch (Exception e) {
                        SimpleFunctions.debug(e.getMessage());
                        Toast.makeText(activity, getString(R.string.error) + ": " +
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        break;
                    }

                    new AlertDialog.Builder(activity)
                            .setMessage(getString(R.string.message_saved_to_file) + " " + file.getAbsolutePath())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else {
                    Toast.makeText(activity, file.getAbsolutePath() + " " + getString(R.string.unable_to_write_error), Toast.LENGTH_SHORT).show();
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        AbstractTransport transport;
        Activity callingActivity;
        String echoarea = "no.echo";
        int total_count;
        int visibleItems = 20;
        int lastVisibleItem;
        int nodeIndex;
        int lastSelectedItem = 0;
        boolean loading, isTablet;
        int primaryColor, secondaryColor, normalItemColor, selectedItemColor;
        private ArrayList<String> msglist;
        private ArrayList<String> visible_msglist;
        private Handler handler;
        private Drawable starredDrawable, unstarredDrawable;
        private RecyclerView rv;

        MyAdapter(Activity activity,
                  RecyclerView recyclerView,
                  ArrayList<String> hashes,
                  AbstractTransport db,
                  String echo,
                  int nodeindex,
                  int startPosition) {
            msglist = hashes;
            transport = db;
            nodeIndex = nodeindex;
            echoarea = echo;
            callingActivity = activity;
            isTablet = SimpleFunctions.isTablet(callingActivity);

            total_count = msglist.size();

            if (total_count < visibleItems || total_count <= startPosition + 1) {
                visible_msglist = new ArrayList<>(msglist);
            } else {
                visible_msglist = new ArrayList<>(msglist.subList(0,
                        startPosition > visibleItems - 1 ? startPosition + 1 : visibleItems));
            }

            handler = new Handler();
            rv = recyclerView;

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
                    }, 1);
                }
            });

            int accentColor = SimpleFunctions.colorFromTheme(callingActivity, R.attr.colorAccent);
            secondaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorSecondary);
            primaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorPrimary);
            normalItemColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.itemBackground);
            selectedItemColor = Color.argb(45, Color.red(secondaryColor),
                    Color.green(secondaryColor), Color.blue(secondaryColor));

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
                    setSelection(holder.position);

                    if (!isTablet) {
                        ArrayList<String> normalMsglist = new ArrayList<>(msglist);
                        Collections.reverse(normalMsglist);

                        Intent intent = new Intent(callingActivity, MessageSlideActivity.class);
                        intent.putExtra("msglist", normalMsglist);

                        if (IDECFunctions.isRealEchoarea(echoarea)) {
                            intent.putExtra("echoarea", echoarea);
                            intent.putExtra("nodeindex", nodeIndex);
                        }
                        intent.putExtra("position", pos);
                        alreadyOpenedSliderActivity = true;
                        callingActivity.startActivityForResult(intent, 1);
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
                    MessageSlideFragment slider = (MessageSlideFragment) ((AppCompatActivity) callingActivity)
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.messages_slider);

                    if (slider != null &&
                            ((total_count - holder.position - 1) == slider.mPager.getCurrentItem()))
                        slider.setStarredIcon(message.is_favorite, slider.starredMenuItem);
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            position = holder.getAdapterPosition();

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

            if (position == lastSelectedItem) {
                holder.itemView.setBackgroundColor(selectedItemColor);
            } else holder.itemView.setBackgroundColor(normalItemColor);

            holder.msg_subj.setTypeface(null, font_style);
            holder.msg_from_to.setTypeface(null, font_style);
            holder.msg_text.setTypeface(null, font_style);
        }

        @Override
        public int getItemCount() {
            return visible_msglist.size();
        }

        void messageChanged(String msgid, boolean needScroll) {
            if (visible_msglist.contains(msgid)) {
                int pos = visible_msglist.indexOf(msgid);
                notifyItemChanged(pos);

                if (needScroll) {
                    rv.scrollToPosition(pos);
                    setSelection(pos);
                }
            }
        }

        void messageChanged(String msgid) {
            messageChanged(msgid, true);
        }

        void setSelection(final int pos) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RecyclerView.ViewHolder lastItemView = rv.findViewHolderForAdapterPosition(lastSelectedItem);
                    RecyclerView.ViewHolder itemView = rv.findViewHolderForAdapterPosition(pos);

                    if (lastItemView != null)
                        lastItemView.itemView.setBackgroundColor(normalItemColor);
                    if (itemView != null) itemView.itemView.setBackgroundColor(selectedItemColor);
                    lastSelectedItem = pos;
                }
            }, 50);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public String msgid;
            public int position;
            TextView msg_subj, msg_from_to, msg_text, msg_date;
            ImageView msg_star;

            ViewHolder(View myLayout) {
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