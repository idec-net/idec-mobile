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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.GlobalTransport;

public class EcholistFragment extends Fragment {
    ArrayList<String> echoareas;
    int nodeindex = -1;
    AbstractTransport transport;

    RecyclerView recyclerView;
    EcholistFragment.MyAdapter mAdapter = null;
    RecyclerView.LayoutManager mLayoutManager;

    public EcholistFragment() {
    }

    public static EcholistFragment newInstance(ArrayList<String> echolist,
                                               int nodenumber
    ) {
        EcholistFragment fragment = new EcholistFragment();
        Bundle args = new Bundle();
        args.putStringArrayList("echolist", echolist);
        args.putInt("nodeindex", nodenumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            echoareas = getArguments().getStringArrayList("echolist");
            nodeindex = getArguments().getInt("nodeindex");
        } else echoareas = new ArrayList<>();
        transport = GlobalTransport.transport;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_echolist, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.echolist);
        mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        mAdapter = new MyAdapter(getActivity(), echoareas, transport, nodeindex);
        recyclerView.setAdapter(mAdapter);
        return rootView;
    }

    public void updateState(ArrayList<String> echolist, int stationIndex) {
        mAdapter.echolist = echolist;
        mAdapter.nodeindex = stationIndex;
        nodeindex = stationIndex;

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit_echoareas) {
            mAdapter.editEchoList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        public ArrayList<String> echolist;
        AbstractTransport transport;
        Activity callingActivity;
        int nodeindex;

        public MyAdapter(Activity activity,
                         ArrayList<String> list,
                         AbstractTransport db,
                         int stationIndex
        ) {
            echolist = list;
            transport = db;
            nodeindex = stationIndex;
            callingActivity = activity;
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent,
                                                       int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.echoarea_list_item_main, parent, false);

            RelativeLayout l = (RelativeLayout) v.findViewById(R.id.echoarea_clickable_layout);

            final ViewHolder holder = new ViewHolder(v);

            l.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewEcho = new Intent(callingActivity, EchoView.class);
                    viewEcho.putExtra("echoarea", holder.echoarea_name.getText().toString());
                    viewEcho.putExtra("nodeindex", nodeindex);

                    callingActivity.startActivity(viewEcho);
                }
            });

            l.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    editEchoList();
                    return true;
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String echoarea = echolist.get(position);
            holder.echoarea_name.setText(echoarea);

            AbstractTransport.echoStat stat = transport.getUnreadStats(echoarea);

            int font_style = (stat.unread_count > 0) ? Typeface.BOLD : Typeface.NORMAL;

            holder.echoarea_name.setTypeface(null, font_style);
            holder.echoarea_unread.setTypeface(null, font_style);

            if (stat.unread_count > 0) {
                holder.echoarea_unread.setVisibility(View.VISIBLE);
                holder.echoarea_unread.setText(String.valueOf(stat.unread_count));
                holder.echoarea_total_count.setText("/" + String.valueOf(stat.total_count));
            } else {
                holder.echoarea_unread.setVisibility(View.GONE);
                holder.echoarea_total_count.setText(String.valueOf(stat.total_count));
            }
        }

        @Override
        public int getItemCount() {
            return echolist.size();
        }

        public void editEchoList() {
            Intent intent = new Intent(callingActivity, ListEditActivity.class);

            if (nodeindex == -1) {
                intent.putExtra("type", "offline");
            } else {
                intent.putExtra("type", "fromstation");
            }

            intent.putExtra("index", nodeindex);
            callingActivity.startActivity(intent);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView echoarea_name, echoarea_unread, echoarea_total_count;

            public ViewHolder(View myLayout) {
                super(myLayout);
                echoarea_name = (TextView) myLayout.findViewById(R.id.echoarea_name);
                echoarea_unread = (TextView) myLayout.findViewById(R.id.echoarea_unread_count);
                echoarea_total_count = (TextView) myLayout.findViewById(R.id.echoarea_total_count);
            }
        }
    }
}