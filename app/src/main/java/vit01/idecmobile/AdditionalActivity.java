package vit01.idecmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.Fetcher;
import vit01.idecmobile.Core.Station;

public class AdditionalActivity extends AppCompatActivity {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Дополнительно");

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    public static class xfile_Fragment extends Fragment {
        public xfile_Fragment() {
        }

        public static xfile_Fragment newInstance() {
            return new xfile_Fragment();
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_additional_xfile, container, false);

            final Spinner spinner = (Spinner) rootView.findViewById(R.id.additional_stations_spinner);

            ArrayList<String> stationNames = new ArrayList<>();
            for (Station station : Config.values.stations) {
                stationNames.add(station.nodename);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, stationNames);
            spinner.setAdapter(adapter);

            final ListView listview = (ListView) rootView.findViewById(R.id.additional_xfile_list);

            Button button = (Button) rootView.findViewById(R.id.additional_load_xfile_list);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), "Загружается, жди!", Toast.LENGTH_SHORT).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            List<HashMap<String, String>> adapter_data = new ArrayList<>();
                            ArrayList<String> lines = Fetcher.xfile_list_download(getContext(),
                                    Config.values.stations.get(spinner.getSelectedItemPosition()));

                            for (String line : lines) {
                                xfile_entry entry = new xfile_entry(line);

                                HashMap<String, String> entryMap = new HashMap<>(2);
                                entryMap.put("First Line", entry.filename);
                                entryMap.put("Second Line", String.valueOf(entry.filesize) + " байт - " + entry.description);
                                adapter_data.add(entryMap);
                            }

                            final SimpleAdapter adapter = new SimpleAdapter(getContext(), adapter_data,
                                    android.R.layout.simple_list_item_2,
                                    new String[]{"First Line", "Second Line"},
                                    new int[]{android.R.id.text1, android.R.id.text2});

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listview.setAdapter(adapter);
                                }
                            });
                        }
                    }).start();
                }
            });

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TextView filename_view = (TextView) view.findViewById(android.R.id.text1);
                    String filename = filename_view.getText().toString();

                    Intent intent = new Intent(getActivity(), DebugActivity.class);
                    intent.putExtra("task", "download_file");
                    intent.putExtra("nodeindex", spinner.getSelectedItemPosition());
                    intent.putExtra("filename", filename);
                    startActivity(intent);
                }
            });

            return rootView;
        }

        public class xfile_entry {
            String filename = "null", description = "null";
            int filesize = 0;

            xfile_entry(String rawline) {
                String[] values = rawline.split(":");
                if (values.length == 3) {
                    filename = values[0];
                    filesize = Integer.parseInt(values[1]);
                    description = values[2];
                }
            }
        }
    }

    public static class Database_Fragment extends Fragment {
        public Database_Fragment() {
        }

        public static Database_Fragment newInstance() {
            return new Database_Fragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_additional_database, container, false);

            return rootView;
        }
    }

    public static class Blacklist_Fragment extends Fragment {
        public Blacklist_Fragment() {
        }

        public static Blacklist_Fragment newInstance() {
            return new Blacklist_Fragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_additional_blacklist, container, false);

            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return xfile_Fragment.newInstance();
                case 1:
                    return Database_Fragment.newInstance();
                case 2:
                    return Blacklist_Fragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Файлы ноды";
                case 1:
                    return "База данных";
                case 2:
                    return "Чёрный список";
            }
            return null;
        }
    }
}
