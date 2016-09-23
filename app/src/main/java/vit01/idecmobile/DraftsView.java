package vit01.idecmobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.DraftMessage;
import vit01.idecmobile.Core.DraftStorage;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;

public class DraftsView extends AppCompatActivity {
    ArrayList<File> msglist;
    boolean unsent_only = true;
    int countMessages;
    ArrayList<String> stations_outbox_id_list = new ArrayList<>();

    RecyclerView recyclerView;
    RecyclerView.Adapter mAdapter = null;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        DraftStorage.initStorage();

        for (Station station : Config.values.stations) {
            stations_outbox_id_list.add(station.outbox_storage_id);
        }

        recyclerView = (RecyclerView) findViewById(R.id.drafts_list_view);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        Intent intent = getIntent();
        unsent_only = intent.getBooleanExtra("unsent", true);

        loadContent(unsent_only);
    }

    boolean loadContent(boolean unsent) {
        msglist = DraftStorage.getAllEntries(unsent);

        if (countMessages == 0) {
            TextView this_is_empty = new TextView(this);
            this_is_empty.setText("Здесь пусто!");
            this_is_empty.setTextSize(20);
            this_is_empty.setPadding(10, 10, 10, 10);
            RelativeLayout currentLayout = (RelativeLayout)
                    findViewById(R.id.draftslist_view_layout);

            currentLayout.addView(this_is_empty, 0);
            return false;
        } else {
            Collections.reverse(msglist);

            mAdapter = new MyAdapter(this, recyclerView, msglist, stations_outbox_id_list);
            recyclerView.setAdapter(mAdapter);
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

    /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
    }*/

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        Activity callingActivity;
        int total_count;
        int visibleItems = 5;
        int lastVisibleItem;
        boolean loading;
        private ArrayList<File> msglist;
        private ArrayList<File> visible_msglist;
        private ArrayList<String> outbox_storage_list;
        private Handler handler;

        public MyAdapter(Activity activity,
                         RecyclerView recyclerView,
                         ArrayList<File> files,
                         ArrayList<String> outbox_id_list
        ) {
            msglist = files;
            callingActivity = activity;
            outbox_storage_list = outbox_id_list;

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
                    }, 500);
                }
            });
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_list_element, parent, false);

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.draft_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(callingActivity, DraftEditor.class);
                    intent.putExtra("task", "edit_existing");
                    intent.putExtra("file", msglist.get(holder.position));
                    intent.putExtra("nodeindex", holder.draft_storage_index);
                    callingActivity.startActivity(intent);
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.position = position;
            File file = msglist.get(position);
            DraftMessage message = DraftStorage.readFromFile(file);
            if (message == null) message = new DraftMessage();

            String storage_id = file.getParentFile().getName();
            if (outbox_storage_list.contains(storage_id)) {
                holder.draft_storage_index = outbox_storage_list.indexOf(storage_id);
            } else {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(callingActivity);
                holder.draft_storage_index = sharedPref.getInt("nodeindex_current", 0);
            }

            holder.draft_subj.setText(message.subj);
            holder.draft_to.setText("to " + message.to);
            holder.draft_text.setText(SimpleFunctions.messagePreview(message.msg));
        }

        @Override
        public int getItemCount() {
            return visible_msglist.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView draft_subj, draft_to, draft_text;
            public int position, draft_storage_index;

            public ViewHolder(View myLayout) {
                super(myLayout);
                draft_subj = (TextView) myLayout.findViewById(R.id.draft_subj);
                draft_to = (TextView) myLayout.findViewById(R.id.draft_to);
                draft_text = (TextView) myLayout.findViewById(R.id.draft_text);
            }
        }
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private Drawable divider;

        public DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
            divider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + divider.getIntrinsicHeight();

                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }
    }
}