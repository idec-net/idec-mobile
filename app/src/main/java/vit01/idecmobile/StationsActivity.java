package vit01.idecmobile;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.support.v7.widget.Toolbar;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.Station;

public class StationsActivity extends AppCompatActivity {
    Spinner spinner;
    ArrayList<String> stationNames;
    ArrayAdapter nodenamesAdapter;
    int currentIndex;
    Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        currentIndex = intent.getIntExtra("index", 0);

        stationNames = new ArrayList<>();

        // Setup spinner
        spinner = (Spinner) findViewById(R.id.spinner);
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
                Toast.makeText(StationsActivity.this, "Это последняя станция!", Toast.LENGTH_SHORT).show();
            } else {
                currentIndex = spinner.getSelectedItemPosition();
                Config.values.stations.remove(currentIndex);
                currentIndex = 0;
                updateSpinner();
                spinner.setSelection(currentIndex);
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
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                // Inflate the drop down using the helper's LayoutInflater
                LayoutInflater inflater = mDropDownHelper.getDropDownViewInflater();
                view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            } else {
                view = convertView;
            }

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
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
        CheckBox xc_enable, advanced_ue, pervasive_ue;
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
            nodename = (EditText) fragm.findViewById(R.id.stations_nodename);
            address = (EditText) fragm.findViewById(R.id.stations_address);
            authstr = (EditText) fragm.findViewById(R.id.stations_authstr);
            fetch_enable = (Switch) fragm.findViewById(R.id.stations_fetch_enable);
            xc_enable = (CheckBox) fragm.findViewById(R.id.stations_xc_enable);
            advanced_ue = (CheckBox) fragm.findViewById(R.id.stations_advanced_ue);
            pervasive_ue = (CheckBox) fragm.findViewById(R.id.stations_pervasive_ue);
            fetch_limit = (EditText) fragm.findViewById(R.id.stations_fetch_limit);
            cut_remote_index = (EditText) fragm.findViewById(R.id.stations_cut_remote_index);
            get_echolist = (Button) fragm.findViewById(R.id.stations_get_echolist);
            autoconfig = (Button) fragm.findViewById(R.id.stations_autoconfig);

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
                    Toast.makeText(getContext(), "Пока что это не работает", Toast.LENGTH_SHORT).show();
                }
            });

            autoconfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), "И это тоже не работает. Жди!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        protected void installValues(int index) {
            station = Config.values.stations.get(index);

            nodename.setText(station.nodename);
            address.setText(station.address);
            authstr.setText(station.authstr);
            fetch_enable.setChecked(station.fetch_enabled);
            xc_enable.setChecked(station.xc_enable);
            advanced_ue.setChecked(station.advanced_ue);
            pervasive_ue.setChecked(station.pervasive_ue);
            fetch_limit.setText(String.valueOf(station.ue_limit));
            cut_remote_index.setText(String.valueOf(station.cut_remote_index));
        }

        protected void fetchValues() {
            station.nodename = nodename.getText().toString();
            station.address = address.getText().toString();
            station.authstr = authstr.getText().toString();
            station.fetch_enabled = fetch_enable.isChecked();
            station.xc_enable = xc_enable.isChecked();
            station.advanced_ue = advanced_ue.isChecked();
            station.pervasive_ue = pervasive_ue.isChecked();
            station.ue_limit = Integer.parseInt(fetch_limit.getText().toString());
            station.cut_remote_index = Integer.parseInt(cut_remote_index.getText().toString());
        }
    }
}