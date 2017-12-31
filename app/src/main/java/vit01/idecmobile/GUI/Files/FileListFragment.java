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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.FEchoFile;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.ProgressActivity;
import vit01.idecmobile.R;
import vit01.idecmobile.gui_helpers.DividerItemDecoration;

public class FileListFragment extends Fragment {
    String echoarea;
    ArrayList<String> filelist;
    int countMessages;
    int nodeIndex;
    RecyclerView recyclerView;
    FileListFragment.MyAdapter mAdapter = null;
    RecyclerView.LayoutManager mLayoutManager;
    int gotPosition = -1;
    String sortType;

    public FileListFragment() {
    }

    public static FileListFragment newInstance(

    ) {
        return new FileListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_filelist, container, false);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent additionalIntent = new Intent(getActivity(), UploaderActivity.class);
                additionalIntent.putExtra("fecho", echoarea);
                startActivity(additionalIntent);
            }
        });

        Activity activity = getActivity();
        IconicsDrawable upload_icon = new IconicsDrawable(activity).icon(GoogleMaterial.Icon.gmd_file_upload)
                .color(SimpleFunctions.colorFromTheme(activity, R.attr.fabIconColor)).sizeDp(19);
        fab.setImageDrawable(upload_icon);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.filelist_view);
        mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(rootView.getContext()));
        recyclerView.setHasFixedSize(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void initFEchoView(String echo, ArrayList<String> msgids, int nIndex, String sort) {
        echoarea = echo;
        filelist = msgids;
        nodeIndex = nIndex;
        sortType = sort;

        // Восстанавливаем состояние фрагмента, если было показано окошко "здесь пусто"
        ViewGroup current = (RelativeLayout) getActivity().findViewById(R.id.filelist_view_layout);
        if (current.findViewById(R.id.content_empty_layout) != null) {
            current.removeAllViews();
            current.removeAllViewsInLayout();
            recyclerView = new RecyclerView(current.getContext());
            mLayoutManager = new LinearLayoutManager(current.getContext());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
            recyclerView.addItemDecoration(new DividerItemDecoration(current.getContext()));
            recyclerView.setHasFixedSize(true);
            recyclerView.scrollToPosition(0);
            current.addView(recyclerView);
            SimpleFunctions.debug("Empty view showed");
        }
        loadContent();
    }

    public void showEmptyView() {
        Activity activity = getActivity();

        View view = activity.getLayoutInflater().inflate(R.layout.content_empty, null, false);
        RelativeLayout l = (RelativeLayout) view.findViewById(R.id.content_empty_layout);
        ((CoordinatorLayout) view.getRootView()).removeAllViews();

        ViewGroup current = (RelativeLayout) activity.findViewById(R.id.filelist_view_layout);

        current.removeAllViews();
        current.addView(l);
    }

    boolean loadContent() {
        ArrayList<String> tmp_filelist;
        Activity activity = getActivity();
        Intent currentIntent = activity.getIntent();

        String requestedFile = currentIntent.getStringExtra("fid");
        if (requestedFile != null) currentIntent.removeExtra("fid");
        // Зануляем fid, см. MessageListFragment

        if (filelist == null) {
            tmp_filelist = GlobalTransport.transport.getFileList(echoarea, 0, 0, sortType);
            currentIntent.putExtra("filelist", tmp_filelist);
            activity.setIntent(currentIntent);
        } else {
            tmp_filelist = filelist;
            Collections.reverse(tmp_filelist);
        }

        int tmp_countMessages = tmp_filelist.size();

        if (tmp_countMessages == 0) {
            filelist = tmp_filelist;
            countMessages = 0;

            showEmptyView();
        }

        filelist = tmp_filelist;
        countMessages = tmp_countMessages;
        Collections.reverse(filelist);

        if (gotPosition < 0 || gotPosition >= filelist.size()) {
            gotPosition = 0;
        }

        mAdapter = new FileListFragment.MyAdapter(activity, this, recyclerView, filelist, GlobalTransport.transport,
                echoarea, nodeIndex, gotPosition);
        recyclerView.setAdapter(mAdapter);
        recyclerView.scrollToPosition(gotPosition);

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (countMessages <= 1) {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            if (searchItem != null) searchItem.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    static class MyAdapter extends RecyclerView.Adapter<FileListFragment.MyAdapter.ViewHolder> {
        AbstractTransport transport;
        Activity callingActivity;
        String echoarea = "no.echo";
        int total_count;
        int visibleItems = 20;
        int lastVisibleItem;
        int nodeIndex;
        boolean loading, isTablet;
        int primaryColor, secondaryColor, normalItemColor, selectedItemColor;
        FileListFragment fragm;
        private ArrayList<String> msglist;
        private ArrayList<String> visible_msglist;
        private Handler handler;
        private Drawable downloadDrawable, deleteDrawable, fixDrawable;

        MyAdapter(Activity activity,
                  FileListFragment fragment,
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
            fragm = fragment;

            total_count = msglist.size();

            if (total_count < visibleItems || total_count <= startPosition + 1) {
                visible_msglist = new ArrayList<>(msglist);
            } else {
                visible_msglist = new ArrayList<>(msglist.subList(0,
                        startPosition > visibleItems - 1 ? startPosition + 1 : visibleItems));
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
                    }, 1);
                }
            });

            int accentColor = SimpleFunctions.colorFromTheme(callingActivity, R.attr.colorAccent);
            secondaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorSecondary);
            primaryColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.textColorPrimary);
            normalItemColor = SimpleFunctions.colorFromTheme(callingActivity, android.R.attr.itemBackground);
            selectedItemColor = Color.argb(45, Color.red(secondaryColor),
                    Color.green(secondaryColor), Color.blue(secondaryColor));


            downloadDrawable = new IconicsDrawable(activity)
                    .icon(GoogleMaterial.Icon.gmd_file_download)
                    .color(primaryColor)
                    .sizeDp(24);

            deleteDrawable = new IconicsDrawable(activity)
                    .icon(GoogleMaterial.Icon.gmd_close)
                    .color(primaryColor)
                    .sizeDp(24);

            fixDrawable = new IconicsDrawable(activity)
                    .icon(GoogleMaterial.Icon.gmd_warning)
                    .color(accentColor)
                    .sizeDp(24);
        }

        @Override
        public FileListFragment.MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                        int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fecho_file_list_element, parent, false);

            final LinearLayout l = (LinearLayout) v.findViewById(R.id.fecho_file_clickable_layout);
            final LinearLayout actionLayout = (LinearLayout) v.findViewById(R.id.fecho_action_layout);

            final FileListFragment.MyAdapter.ViewHolder holder = new FileListFragment.MyAdapter.ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragm.gotPosition = holder.getAdapterPosition();

                    if (holder.entry == null) return;

                    boolean exists = holder.exists;
                    boolean sizeCorrect = holder.sizeIsCorrect;

                    if (exists && sizeCorrect) {
                        performOpenFile(holder);
                    } else if (exists && !sizeCorrect) {
                        String serverReportedSize = holder.fecho_filesize.getText().toString();
                        long fsize = holder.entry.getLocalFile().length();
                        String localSize = Formatter.formatFileSize(callingActivity, fsize);
                        if (serverReportedSize.equals(localSize)) localSize = String.valueOf(fsize);

                        new AlertDialog.Builder(callingActivity)
                                .setTitle(R.string.data_is_corrupted)
                                .setMessage(callingActivity.getString(R.string.file_size_mismatch, localSize, serverReportedSize))
                                .setPositiveButton(R.string.open_file_anyway, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        performOpenFile(holder);
                                    }
                                })
                                .setNeutralButton(R.string.action_file_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        performDeletion(holder);
                                    }
                                })
                                .show();
                    } else if (!exists) {
                        Intent downloadFile = new Intent(callingActivity, ProgressActivity.class);
                        downloadFile.putExtra("task", "download_fp");
                        downloadFile.putExtra("fid", holder.fid);
                        downloadFile.putExtra("filesize", holder.entry.serverSize);
                        downloadFile.putExtra("nodeindex", nodeIndex);
                        callingActivity.startActivity(downloadFile);
                    }
                }
            });

            l.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(R.string.file_show_description)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    new AlertDialog.Builder(callingActivity)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .setTitle(holder.entry.filename)
                                            .setMessage(holder.entry.description)
                                            .show();
                                    return true;
                                }
                            });
                    menu.add(R.string.fid_clipboard_copy)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    ClipboardManager clipboard = (ClipboardManager)
                                            callingActivity.getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("idec file id", holder.fid);
                                    clipboard.setPrimaryClip(clip);

                                    Toast.makeText(callingActivity, R.string.fid_clipboard_done, Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                            });
                    menu.add(R.string.action_share)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    performShare(holder);
                                    return true;
                                }
                            });
                }
            });

            actionLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.exists && holder.sizeIsCorrect) {
                        new AlertDialog.Builder(callingActivity)
                                .setTitle(R.string.action_file_delete)
                                .setMessage(callingActivity.getString(
                                        R.string.confirm_delete_file, holder.entry.filename))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        performDeletion(holder);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    } else l.callOnClick();
                }
            });

            return holder;
        }

        void performOpenFile(final FileListFragment.MyAdapter.ViewHolder holder) {
            Intent openfile = new Intent();
            openfile.setAction(Intent.ACTION_VIEW);
            Uri fileUri = FileProvider.getUriForFile(
                    callingActivity.getApplicationContext(), "vit01.idecmobile.provider", holder.entry.getLocalFile());
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(fileUri.toString()));
            openfile.setDataAndType(fileUri, mime);
            openfile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            callingActivity.startActivity(openfile);
        }

        void performShare(final FileListFragment.MyAdapter.ViewHolder holder) {
            if (!holder.exists) {
                Toast.makeText(callingActivity, R.string.no_file_warning, Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(
                    callingActivity.getApplicationContext(), "vit01.idecmobile.provider", holder.entry.getLocalFile());

            Intent shareIntent = ShareCompat.IntentBuilder.from(callingActivity)
                    .setType(callingActivity.getContentResolver().getType(fileUri))
                    .setStream(fileUri)
                    .getIntent();

            shareIntent.setData(fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (shareIntent.resolveActivity(callingActivity.getPackageManager()) != null) {
                callingActivity.startActivity(shareIntent);
            } else Toast.makeText(callingActivity, R.string.error, Toast.LENGTH_SHORT).show();
        }

        void performDeletion(final FileListFragment.MyAdapter.ViewHolder holder) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean r = holder.entry.getLocalFile().delete();
                    callingActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(holder.getAdapterPosition());
                            Toast.makeText(callingActivity,
                                    r ? R.string.done : R.string.deletion_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        }

        @Override
        public void onBindViewHolder(FileListFragment.MyAdapter.ViewHolder holder, int position) {
            position = holder.getAdapterPosition();

            holder.fid = visible_msglist.get(position);
            holder.position = position;
            FEchoFile entry = transport.getFileMeta(holder.fid);
            if (entry == null) entry = new FEchoFile();
            holder.exists = entry.existsLocally();
            holder.sizeIsCorrect = entry.localSizeIsCorrect();
            holder.entry = entry;

            holder.fecho_filename.setText(entry.filename);
            holder.fecho_address.setText(entry.addr);
            holder.fecho_description.setText(entry.description);
            holder.fecho_filesize.setText(Formatter.formatFileSize(callingActivity, entry.serverSize));

            holder.fecho_description.setTextColor(secondaryColor);

            if (holder.exists && holder.sizeIsCorrect) {
                holder.action_button.setImageDrawable(deleteDrawable);
            } else if (holder.exists && !holder.sizeIsCorrect) {
                holder.action_button.setImageDrawable(fixDrawable);
            } else if (!holder.exists) {
                holder.action_button.setImageDrawable(downloadDrawable);
            }
        }

        @Override
        public int getItemCount() {
            return visible_msglist.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public int position;
            String fid;
            FEchoFile entry;
            TextView fecho_filename, fecho_address, fecho_description, fecho_filesize;
            ImageView action_button;
            boolean exists, sizeIsCorrect;

            ViewHolder(View myLayout) {
                super(myLayout);
                fecho_filename = (TextView) myLayout.findViewById(R.id.fecho_filename);
                fecho_address = (TextView) myLayout.findViewById(R.id.fecho_address);
                fecho_description = (TextView) myLayout.findViewById(R.id.fecho_description);
                fecho_filesize = (TextView) myLayout.findViewById(R.id.fecho_filesize);
                action_button = (ImageView) myLayout.findViewById(R.id.fecho_action_button);
            }
        }
    }
}
