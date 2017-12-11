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

package vit01.idecmobile.GUI.Files;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;
import vit01.idecmobile.gui_helpers.DividerItemDecoration;
import vit01.idecmobile.prefs.Config;

public class FileChooserActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    LinearLayoutManager mLayoutManager;
    File currentDir;
    FilesAdapter mAdapter;
    int textColorPrimary;

    private static File[] sortFiles(File[] files) {
        Comparator<File> comp = new Comparator<File>() {
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) {
                    return -1;
                } else if (!f1.isDirectory() && f2.isDirectory()) {
                    return 1;
                } else {
                    return f1.compareTo(f2);
                }
            }
        };
        Arrays.sort(files, comp);
        return files;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, getString(R.string.title_activity_file_chooser));

        textColorPrimary = SimpleFunctions.colorFromTheme(this, android.R.attr.textColorPrimary);
        currentDir = getExternalOrRoot();

        recyclerView = (RecyclerView) findViewById(R.id.file_chooser_entries);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        mAdapter = new FilesAdapter(this, recyclerView, currentDir, textColorPrimary);
        recyclerView.setAdapter(mAdapter);
    }

    private File getExternalOrRoot() {
        Boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isSDPresent) {
            return Environment.getExternalStorageDirectory().getAbsoluteFile();
        } else {
            return new File("/");
        }
    }

    @Override
    public void onBackPressed() {
        if (!mAdapter.goUp()) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
        Activity callingActivity;
        int total_count;
        int visibleItems = 5;
        int lastVisibleItem;
        boolean loading;
        private File currentdir;
        private ArrayList<File> filelist = new ArrayList<>();
        private ArrayList<File> visible_filelist = new ArrayList<>();
        private Handler handler;
        private Drawable directory_icon;
        private Drawable file_icon;

        public FilesAdapter(Activity activity,
                            RecyclerView recyclerView,
                            File currentDir,
                            int iconsColor
        ) {
            selectItem(currentDir);
            callingActivity = activity;

            handler = new Handler();
            directory_icon = new IconicsDrawable(callingActivity).icon(GoogleMaterial.Icon.gmd_folder).color(iconsColor);
            file_icon = new IconicsDrawable(callingActivity).icon(GoogleMaterial.Icon.gmd_insert_drive_file).color(iconsColor);

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

                                    visible_filelist.add(filelist.get(itemToInsert));
                                    notifyItemInserted(itemToInsert);
                                }
                                loading = false;
                            }
                        }
                    }, 10);
                }
            });
        }

        @Override
        public FilesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_list_element, parent, false);

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.file_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File chosen = filelist.get(holder.getAdapterPosition());
                    selectItem(chosen);
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            File file = filelist.get(position);
            holder.filename.setText(file.getName());
            holder.filesize.setText(Formatter.formatFileSize(callingActivity, file.length()));
            if (file.isDirectory()) {
                holder.fileIcon.setImageDrawable(directory_icon);
            } else holder.fileIcon.setImageDrawable(file_icon);
        }

        @Override
        public int getItemCount() {
            return visible_filelist.size();
        }

        public void selectItem(File file) {
            if (file.isDirectory()) {
                currentdir = file;
                filelist.clear();
                visible_filelist.clear();

                File[] contents = file.listFiles();
                if (contents != null) {
                    filelist.addAll(Arrays.asList(sortFiles(contents)));
                }

                if (file.getParent() != null) {
                    filelist.add(0, new File(".."));
                }

                total_count = filelist.size();

                if (total_count < visibleItems) {
                    visible_filelist.addAll(filelist);
                } else {
                    visible_filelist.addAll(filelist.subList(0, visibleItems));
                }
                notifyDataSetChanged();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_file", file);
                callingActivity.setResult(RESULT_OK, resultIntent);
                callingActivity.finish();
            }
        }

        public boolean goUp() {
            if (currentdir.getParentFile() != null) {
                selectItem(currentdir.getParentFile());
                return true;
            } else return false;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView filename, filesize;
            public ImageView fileIcon;

            public ViewHolder(View myLayout) {
                super(myLayout);
                filename = (TextView) myLayout.findViewById(R.id.file_item_name);
                filesize = (TextView) myLayout.findViewById(R.id.file_item_size);
                fileIcon = (ImageView) myLayout.findViewById(R.id.file_item_icon);
            }
        }
    }
}