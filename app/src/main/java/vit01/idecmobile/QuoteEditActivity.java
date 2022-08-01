/*
 * Copyright (c) 2016-2018 Viktor Fedenyov <me@alicorn.tk> <https://alicorn.tk>
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import vit01.idecmobile.Core.DraftMessage;
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.GUI.Drafts.DraftEditor;
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
        SimpleFunctions.setActivityTitle(this, getString(R.string.quote_dialog_title));

        ExternalStorage.initStorage();

        Intent in = getIntent();
        message_output = new DraftMessage();
        message_input = (IIMessage) in.getSerializableExtra("message");
        message_output.echo = message_input.echo;
        message_output.to = message_input.from;
        message_output.subj = SimpleFunctions.subjAnswer(message_input.subj);
        message_output.repto = message_input.id;

        message_output.msg = SimpleFunctions.quoteAnswer
                (message_input.msg, message_output.to, Config.values.oldQuote);

        nodeindex = in.getIntExtra("nodeindex", 0);
        outbox_id = Config.values.stations.get(nodeindex).outbox_storage_id;
        fileToSave = ExternalStorage.newMessage(outbox_id, message_output);

        final FloatingActionButton fab = findViewById(R.id.fab_ev);
        fab.setVisibility(View.GONE);

        contents = new ArrayList<>(Arrays.asList(message_output.msg.split("\n\n")));
        for (String str : contents) {
            if (str.equals("") || str.equals("\n")) contents.remove(str);
        }

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

    public void saveMessage() {
        message_output.msg = TextUtils.join("\n\n", contents_adapter.elements);
        boolean result = ExternalStorage.writeDraftToFile(fileToSave, message_output.raw());
        if (!result) {
            SimpleFunctions.debug(getString(R.string.error));
            Toast.makeText(QuoteEditActivity.this, R.string.unable_to_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(QuoteEditActivity.this)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.quote_dialog_save_or_exit)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(QuoteEditActivity.this, R.string.delete_draft, Toast.LENGTH_SHORT).show();
                        boolean r = fileToSave.delete();
                        if (!r) {
                            Toast.makeText(QuoteEditActivity.this, R.string.deletion_error, Toast.LENGTH_SHORT).show();
                        } else fileToSave = null;
                        finish();
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveMessage();
                        finish();
                    }
                })
                .setNeutralButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_quote_compose, menu);

        Context context = getApplicationContext();

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        menu.findItem(R.id.action_quote_save).setIcon(new IconicsDrawable
                (context, GoogleMaterial.Icon.gmd_check).actionBar().color(iconColor));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_quote_save) {
            saveMessage();
            Intent editor = new Intent(QuoteEditActivity.this, DraftEditor.class);
            editor.putExtra("nodeindex", nodeindex);
            editor.putExtra("task", "edit_existing");
            editor.putExtra("file", fileToSave);
            startActivity(editor);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public class ListEditAdapter extends RecyclerView.Adapter<ListEditAdapter.ViewHolder> {
        RecyclerView recyclerView;
        Activity callingActivity;
        ArrayList<String> elements;
        int replaceindex;

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
                    .inflate(R.layout.quote_list_item, parent, false);

            RelativeLayout l = v.findViewById(R.id.quote_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);
            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int position = holder.getAdapterPosition();
                    TextView content = view.findViewById(R.id.quote_text);
                    final String[] strelements = content.getText().toString().split("\n");
                    if (strelements.length == 1) return;

                    new AlertDialog.Builder(QuoteEditActivity.this)
                            .setTitle(R.string.title_break_quote)
                            .setSingleChoiceItems(strelements, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    replaceindex = i;
                                    if (i == (strelements.length - 1) && i != 0) {
                                        replaceindex--;
                                    }
                                }
                            })
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    StringBuilder firstFragment = new StringBuilder();
                                    StringBuilder secondFragment = new StringBuilder();

                                    for (int a = 0; a < replaceindex + 1; a++) {
                                        firstFragment.append(strelements[a]).append("\n");
                                    }
                                    firstFragment.deleteCharAt(firstFragment.length() - 1);
                                    // мы просто удалили последний \n

                                    for (int a = replaceindex + 1; a < strelements.length; a++) {
                                        secondFragment.append(strelements[a]).append("\n");
                                    }
                                    secondFragment.deleteCharAt(secondFragment.length() - 1);

                                    elements.remove(position);
                                    elements.add(position, firstFragment.toString());
                                    elements.add(position + 1, secondFragment.toString());
                                    notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView t1 = holder.itemView.findViewById(R.id.quote_text);
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