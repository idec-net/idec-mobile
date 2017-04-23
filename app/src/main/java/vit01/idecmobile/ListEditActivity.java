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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.SimpleFunctions;

public class ListEditActivity extends AppCompatActivity {
    ArrayList<String> contents;
    EchoListEditAdapter contents_adapter;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    ListView action;
    EditText echoEdit;
    AlertDialog editEchoarea;
    int echoPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, "Правка списка эх");

        Intent in = getIntent();
        String listType = in.getStringExtra("type");

        switch (listType) {
            case "fromstation":
                int stationIndex = in.getIntExtra("index", 0);
                contents = Config.values.stations.get(stationIndex).echoareas;
                break;
            case "offline":
                contents = Config.values.offlineEchoareas;
                break;
        }

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_ev);
        IconicsDrawable add_icon = new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_add).color(Color.WHITE).sizeDp(16);
        fab.setImageDrawable(add_icon);


        recyclerView = (RecyclerView) findViewById(R.id.contents);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        contents_adapter = new EchoListEditAdapter(this, recyclerView, contents);

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(contents_adapter);

        ItemTouchHelper.Callback touchCallback = new ItemTouchHelper.Callback() {
            int itemHeight, iconTop, iconBottom, iconLeft, iconRight;
            boolean viewBeingCleared = false;

            Drawable background = new ColorDrawable(SimpleFunctions.colorFromTheme(ListEditActivity.this, R.attr.colorAccent));
            Drawable icon = new IconicsDrawable(ListEditActivity.this)
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

        final View alertView = getLayoutInflater().inflate(R.layout.alert_echo_menu, null);

        echoEdit = (EditText) alertView.findViewById(R.id.edit_echoarea);
        action = (ListView) alertView.findViewById(R.id.echo_menu_options);

        fab.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       String gotText = "echoarea.new";
                                       contents.add(gotText);
                                       echoPosition = contents.size() - 1;
                                       contents_adapter.notifyItemInserted(echoPosition);
                                       recyclerView.smoothScrollToPosition(echoPosition);

                                       echoEdit.setText(gotText);
                                       editEchoarea.show();
                                   }
                               }
        );

        editEchoarea = new AlertDialog.Builder(ListEditActivity.this)
                .setTitle("Правка эхоконференции")
                .setView(alertView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String gotText = echoEdit.getText().toString().trim();
                        if (!gotText.equals("")) {
                            contents.set(echoPosition, gotText);
                            contents_adapter.notifyItemChanged(echoPosition);
                        }
                    }
                }).create();

        action.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String element;

                switch (position) {
                    case 0:
                        element = contents.remove(echoPosition);
                        contents.add(0, element);
                        echoPosition = 0;
                        break;
                    case 1:
                        element = contents.remove(echoPosition);
                        contents.add(element);
                        echoPosition = contents.size() - 1;
                        break;
                    case 2:
                        if (contents.size() > 1) {
                            contents.remove(echoPosition);
                            editEchoarea.cancel();
                        } else {
                            Toast.makeText(ListEditActivity.this,
                                    "В списке должна быть хотя бы одна эхоконференция!",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                contents_adapter.notifyDataSetChanged();
            }
        });
        Toast.makeText(ListEditActivity.this, "Перемещаем и удяляем эхи через drag&drop!", Toast.LENGTH_SHORT).show();
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

    public class EchoListEditAdapter extends RecyclerView.Adapter<EchoListEditAdapter.ViewHolder> {
        RecyclerView recyclerView;
        Activity callingActivity;
        ArrayList<String> elements;

        public EchoListEditAdapter(Activity activity,
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

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.echoarea_edit_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);
            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    echoPosition = holder.getAdapterPosition();
                    TextView content = (TextView) view.findViewById(R.id.echoarea_edit_name);
                    echoEdit.setText(content.getText());
                    editEchoarea.show();
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView t1 = (TextView) holder.itemView.findViewById(R.id.echoarea_edit_name);
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