/*
 * Copyright (c) 2016-2022 Viktor Fedenyov <me@alicorn.tk> <https://alicorn.tk>
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

package vit01.idecmobile.GUI.Settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ThemedSpinnerAdapter;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.Network;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class StationsActivity extends AppCompatActivity {
    public static boolean download_all_warning = true;
    Spinner spinner;
    ArrayList<String> stationNames;
    ArrayAdapter nodenamesAdapter;
    int currentIndex;
    Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        currentIndex = intent.getIntExtra("index", 0);

        stationNames = new ArrayList<>();

        // Setup spinner
        spinner = findViewById(R.id.spinner);
        nodenamesAdapter = new MyAdapter(toolbar.getContext(), stationNames);
        spinner.setAdapter(nodenamesAdapter);

        updateSpinner();

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // When the given dropdown item is selected, show its contents in the
                // container view.
                currentFragment = PlaceholderFragment.newInstance(position);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, currentFragment)
                        .commit();
                updateSpinner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateSpinner();
            }
        });

        spinner.setSelection(currentIndex);
        ExternalStorage.initStorage();
    }

    @Override
    public void onBackPressed() {
        currentFragment.onStop();
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.delete_station) {
            if (Config.values.stations.size() == 1) {
                Toast.makeText(StationsActivity.this, R.string.warning_last_station, Toast.LENGTH_SHORT).show();
            } else {
                currentIndex = spinner.getSelectedItemPosition();

                final File draftsDir = ExternalStorage.getStationStorageDir(Config.values.stations.get(currentIndex).outbox_storage_id);
                final ArrayList<File> contents = ExternalStorage.getDraftsInside(draftsDir, true);
                contents.addAll(ExternalStorage.getDraftsInside(draftsDir, false));

                if (contents.size() > 0) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.delete_node)
                            .setMessage(R.string.delete_node_confirm)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    for (File file : contents) {
                                        boolean result = file.delete();
                                        if (!result)
                                            SimpleFunctions.debug("Error deleting file " + file.getName());
                                    }

                                    if (draftsDir != null) {
                                        boolean r = draftsDir.delete();
                                        if (!r)
                                            Toast.makeText(StationsActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                    }

                                    SimpleFunctions.delete_xc_from_station(getApplicationContext(), Config.values.stations.get(currentIndex));
                                    Config.values.stations.remove(currentIndex);
                                    currentIndex = 0;
                                    updateSpinner();
                                    spinner.setSelection(currentIndex);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(StationsActivity.this, android.R.string.ok, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                } else {
                    if (draftsDir != null) {
                        boolean r = draftsDir.delete();
                        if (!r)
                            Toast.makeText(StationsActivity.this, R.string.draft_dir_deletion_error, Toast.LENGTH_SHORT).show();
                    }

                    SimpleFunctions.delete_xc_from_station(getApplicationContext(), Config.values.stations.get(currentIndex));
                    Config.values.stations.remove(currentIndex);
                    currentIndex = 0;
                    updateSpinner();
                    spinner.setSelection(currentIndex);
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void updateSpinner() {
        stationNames.clear();
        for (Station station : Config.values.stations) {
            stationNames.add(station.nodename);
        }

        nodenamesAdapter.notifyDataSetChanged();
    }

    private static class MyAdapter extends ArrayAdapter<String> implements ThemedSpinnerAdapter {
        private final ThemedSpinnerAdapter.Helper mDropDownHelper;

        public MyAdapter(Context context, ArrayList<String> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            mDropDownHelper = new ThemedSpinnerAdapter.Helper(context);
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;

            if (convertView == null) {
                // Inflate the drop down using the helper's LayoutInflater
                LayoutInflater inflater = mDropDownHelper.getDropDownViewInflater();
                view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            } else {
                view = convertView;
            }

            TextView textView = view.findViewById(android.R.id.text1);
            textView.setText(getItem(position));

            return view;
        }

        @Override
        public Theme getDropDownViewTheme() {
            return mDropDownHelper.getDropDownViewTheme();
        }

        @Override
        public void setDropDownViewTheme(Theme theme) {
            mDropDownHelper.setDropDownViewTheme(theme);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        int currentIndex;

        Station station;
        EditText nodename, address, authstr, fetch_limit, cut_remote_index;
        Switch fetch_enable;
        CheckBox xc_enable, advanced_ue, pervasive_ue, show_password, fecho_enable;
        Button get_echolist, autoconfig;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt("station_number", sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_stations, container, false);
            getControls(rootView);
            currentIndex = getArguments().getInt("station_number");
            installValues(currentIndex);
            return rootView;
        }

        @Override
        public void onStop() {
            fetchValues();
            Config.writeConfig(getContext());
            super.onStop();
        }

        protected void getControls(View fragm) {
            nodename = fragm.findViewById(R.id.stations_nodename);
            address = fragm.findViewById(R.id.stations_address);
            authstr = fragm.findViewById(R.id.stations_authstr);
            show_password = fragm.findViewById(R.id.show_password);
            fetch_enable = fragm.findViewById(R.id.stations_fetch_enable);
            xc_enable = fragm.findViewById(R.id.stations_xc_enable);
            advanced_ue = fragm.findViewById(R.id.stations_advanced_ue);
            pervasive_ue = fragm.findViewById(R.id.stations_pervasive_ue);
            fecho_enable = fragm.findViewById(R.id.stations_fecho_support);
            fetch_limit = fragm.findViewById(R.id.stations_fetch_limit);
            cut_remote_index = fragm.findViewById(R.id.stations_cut_remote_index);
            get_echolist = fragm.findViewById(R.id.stations_get_echolist);
            autoconfig = fragm.findViewById(R.id.stations_autoconfig);

            advanced_ue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    pervasive_ue.setEnabled(isChecked);
                    fetch_limit.setEnabled(isChecked);
                }
            });

            get_echolist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String str_address = addressSlashCheck();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), R.string.wait, Toast.LENGTH_SHORT).show();
                                }
                            });

                            final String echolist_info = Network.getFile(getContext(), str_address + "list.txt", null, Config.values.connectionTimeout);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    installEchoList(echolist_info, currentIndex);
                                }
                            });
                        }
                    }).start();
                }
            });

            autoconfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String str_address = addressSlashCheck();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), R.string.connecting_to_station, Toast.LENGTH_SHORT).show();
                                }
                            });

                            final String xfinfo = Network.getFile(getContext(), str_address + "x/features", null, Config.values.connectionTimeout);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    xfeatures_configure(xfinfo);
                                }
                            });
                        }
                    }).start();
                }
            });

            pervasive_ue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b && download_all_warning && !station.pervasive_ue) {
                        download_all_warning = false;

                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.warning)
                                .setMessage(R.string.warning_download_limits)
                                .setPositiveButton(R.string.i_am_really_sure, null)
                                .show();
                    }
                }
            });

            show_password.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    int inputType = b ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD;
                    authstr.setInputType(InputType.TYPE_CLASS_TEXT | inputType);
                }
            });
        }

        public void xfeatures_configure(String xfinfo) {
            if (xfinfo != null) {
                List<String> keys = Arrays.asList(xfinfo.split("\n"));
                boolean xc = keys.contains("x/c");
                boolean ue = keys.contains("u/e");
                boolean fe = keys.contains("f/");

                xc_enable.setChecked(xc);
                advanced_ue.setChecked(ue);
                fecho_enable.setChecked(fe);

                if (ue) {
                    pervasive_ue.setChecked(false);
                }
            } else {
                xc_enable.setChecked(false);
                advanced_ue.setChecked(false);
                fecho_enable.setChecked(false);
            }
            cut_remote_index.setText("50");

            Toast.makeText(getContext(), R.string.autoconfig_done, Toast.LENGTH_SHORT).show();
        }

        public void installEchoList(String rawfile, final int nodeindex) {
            final ArrayList<String> realEchoList = new ArrayList<>();
            final Context mContext = getContext();
            final ListView lv = new ListView(mContext);

            try {
                List<HashMap<String, String>> adapter_data = new ArrayList<>();

                if (rawfile == null) {
                    SimpleFunctions.debug("installEchoList: rawfile = null");
                    Toast.makeText(getActivity(), R.string.interner_error_echolist, Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] lines = rawfile.split("\n");

                for (String line : lines) {
                    echoarea_entry entry = new echoarea_entry(line);
                    if (entry.corrupted) continue;

                    HashMap<String, String> entryMap = new HashMap<>(2);
                    entryMap.put("First Line", entry.name);
                    entryMap.put("Second Line", "[" + entry.count + "] - " + entry.description);
                    adapter_data.add(entryMap);
                    realEchoList.add(entry.name);
                }

                if (realEchoList.size() == 0) {
                    Toast.makeText(mContext, R.string.echolist_parsing_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                final SimpleAdapter adapter = new SimpleAdapter(mContext, adapter_data,
                        android.R.layout.simple_list_item_2,
                        new String[]{"First Line", "Second Line"},
                        new int[]{android.R.id.text1, android.R.id.text2});


                lv.setAdapter(adapter);

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.echolist_confirm_install)
                        .setView(lv)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Config.values.stations.get(nodeindex).echoareas = realEchoList;
                                Config.writeConfig(mContext);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(mContext, R.string.edit_by_yourself, Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            } catch (Exception e) {
                Toast.makeText(mContext, R.string.echolist_parsing_error, Toast.LENGTH_SHORT).show();
                SimpleFunctions.debug(e.getMessage() + e.toString());
            }
        }

        protected void installValues(int index) {
            station = Config.values.stations.get(index);

            nodename.setText(station.nodename);
            address.setText(station.address);
            authstr.setText(station.authstr);
            fetch_enable.setChecked(station.fetch_enabled);
            xc_enable.setChecked(station.xc_enable);
            advanced_ue.setChecked(station.advanced_ue);
            fecho_enable.setChecked(station.fecho_support);
            pervasive_ue.setChecked(station.pervasive_ue);
            fetch_limit.setText(String.valueOf(station.ue_limit));
            cut_remote_index.setText(String.valueOf(station.cut_remote_index));
        }

        protected void fetchValues() {
            addressSlashCheck();
            station.nodename = nodename.getText().toString();
            station.address = address.getText().toString();
            station.authstr = authstr.getText().toString();
            station.fetch_enabled = fetch_enable.isChecked();
            station.xc_enable = xc_enable.isChecked();
            station.advanced_ue = advanced_ue.isChecked();
            station.pervasive_ue = pervasive_ue.isChecked();
            station.fecho_support = fecho_enable.isChecked();
            station.ue_limit = Integer.parseInt(fetch_limit.getText().toString());
            station.cut_remote_index = Integer.parseInt(cut_remote_index.getText().toString());
        }

        protected String addressSlashCheck() {
            String str_address = address.getText().toString();

            if (!str_address.endsWith("/")) {
                str_address += "/";

                final String finalStr_address = str_address;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), R.string.station_slash_at_the_end, Toast.LENGTH_SHORT).show();
                        address.setText(finalStr_address);
                    }
                });
            }

            return str_address;
        }
    }

    public static class echoarea_entry {
        String name = "null", description = "null";
        int count = 0;
        boolean corrupted = false;

        echoarea_entry(String rawline) {
            String[] values = rawline.split(":");
            if (values.length >= 2) {
                name = values[0];
                count = Integer.parseInt(values[1]);

                if (values.length > 2) description = values[2];
                else description = "<null>";
            } else corrupted = true;
        }
    }
}