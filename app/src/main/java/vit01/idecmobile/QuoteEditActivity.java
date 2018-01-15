/*
 * Copyright (c) 2016-2018 Viktor Fedenyov <me@ii-net.tk> <https://ii-net.tk>
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import vit01.idecmobile.Core.DraftMessage;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.gui_helpers.DividerItemDecoration;
import vit01.idecmobile.prefs.Config;

public class QuoteEditActivity extends AppCompatActivity {
    ArrayList<String> contents;
    ListEditAdapter contents_adapter;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    IIMessage message_input;
    DraftMessage message_output;
    int nodeindex;
    String outbox_id;
    File fileToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_edit);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, getString(R.string.title_activity_list_edit));

        ExternalStorage.initStorage();

        Intent in = getIntent();
        message_output = new DraftMessage();
        message_input = (IIMessage) in.getSerializableExtra("message");
        message_output.echo = message_input.echo;
        message_output.to = message_input.from;
        message_output.subj = SimpleFunctions.subjAnswer(message_input.subj);
        message_output.repto = message_input.id;

        message_output.msg = SimpleFunctions.quoteAnswer
                (message_input.msg, message_input.to, Config.values.oldQuote);

        nodeindex = in.getIntExtra("nodeindex", 0);
        outbox_id = Config.values.stations.get(nodeindex).outbox_storage_id;
        fileToSave = ExternalStorage.newMessage(outbox_id, message_output);

        final FloatingActionButton fab = findViewById(R.id.fab_ev);
        fab.setVisibility(View.GONE);

        contents = new ArrayList<>(Arrays.asList(message_output.msg.split("\n")));

        recyclerView = findViewById(R.id.contents);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        contents_adapter = new ListEditAdapter(this, recyclerView, contents);

        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(contents_adapter);

        ItemTouchHelper.Callback touchCallback = new ItemTouchHelper.Callback() {
            int itemHeight, iconTop, iconBottom, iconLeft, iconRight;
            boolean viewBeingCleared = false;

            Drawable background = new ColorDrawable(SimpleFunctions.colorFromTheme(QuoteEditActivity.this, R.attr.colorAccent));
            Drawable icon = new IconicsDrawable(QuoteEditActivity.this)
                    .icon(GoogleMaterial.Icon.gmd_delete)
                    .color(Color.WHITE)
                    .sizeDp(20);
            int iconMargin = (int) getResources().getDimension(R.dimen.list_horizontal_margin);

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END,
                        ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                contents_adapter.onItemMove(fromPos, toPos);
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                viewBeingCleared = true;
                super.clearView(recyclerView, viewHolder);
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                contents_adapter.onItemDismiss(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;

                    itemHeight = itemView.getBottom() - itemView.getTop();
                    iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                    iconBottom = iconTop + icon.getIntrinsicHeight();

                    if (dX > 0) {
                        iconLeft = itemView.getLeft() + iconMargin;
                        iconRight = iconLeft + icon.getIntrinsicWidth();
                        background.setBounds(itemView.getLeft(), itemView.getTop(), (int) dX, itemView.getBottom());
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    } else {
                        iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        iconRight = itemView.getRight() - iconMargin;
                        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }
                    if (dX != 0) {
                        background.draw(c);
                        icon.draw(c);
                    }
                } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (viewBeingCleared) {
                        viewBeingCleared = false;
                        viewHolder.itemView.setAlpha(1f);
                    } else viewHolder.itemView.setAlpha(0.4f);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                viewHolder.itemView.setScaleY(1f);
            }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onBackPressed() {
        Config.writeConfig(this);
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public class ListEditAdapter extends RecyclerView.Adapter<ListEditAdapter.ViewHolder> {
        RecyclerView recyclerView;
        Activity callingActivity;
        ArrayList<String> elements;

        public ListEditAdapter(Activity activity,
                               RecyclerView rv,
                               ArrayList<String> contents) {
            callingActivity = activity;
            elements = contents;
            recyclerView = rv;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.echoarea_list_item_edit, parent, false);

            RelativeLayout l = v.findViewById(R.id.echoarea_edit_clickable_layout);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView t1 = holder.itemView.findViewById(R.id.echoarea_edit_name);
            t1.setText(elements.get(position));
        }

        @Override
        public int getItemCount() {
            return elements.size();
        }

        public void onItemDismiss(int position) {
            elements.remove(position);
            notifyItemRemoved(position);
        }

        public void onItemMove(int from, int to) {
            elements.add(to, elements.remove(from));
            notifyItemMoved(from, to);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}