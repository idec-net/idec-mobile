package vit01.idecmobile;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public Station currentStation;
    public ListView echoList;
    public ArrayAdapter echoListAdapter;
    public int currentStationIndex = 0;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Config.loadConfig(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);
        final Spinner spinner = (Spinner) header.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter;
        List<String> stationsList = new ArrayList<>();

        for (Station station : Config.values.stations) {
            stationsList.add(station.nodename);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, stationsList);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStation = Config.values.stations.get(position);
                currentStationIndex = position;
                updateEcholist();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        currentStation = Config.values.stations.get(0);

        echoList = (ListView) findViewById(R.id.echolist);
        echoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String echoarea = ((TextView) view).getText().toString();
                Intent viewEcho = new Intent(MainActivity.this, EchoView.class);
                viewEcho.putExtra("echoarea", echoarea);
                viewEcho.putExtra("nodeindex", currentStationIndex);
                startActivity(viewEcho);
            }
        });

        updateEcholist();

        if (Config.values.firstRun) {
            Config.values.firstRun = false;
            startActivity(new Intent(this, CommonSettings.class));
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void updateEcholist() {
        echoListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, currentStation.echoareas);

        echoList.setAdapter(echoListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, CommonSettings.class));
            return true;
        } else if (id == R.id.action_fetch) {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra("task", "fetch");
            startActivity(intent);
            return true;
        } else if (id == R.id.action_clear_cache) {
            for (Station station : Config.values.stations) {
                String xc_name = "xc_" + SimpleFunctions.hsh(station.nodename);
                SimpleFunctions.write_internal_file(this, xc_name, "");
            }
            Toast.makeText(MainActivity.this, "Кэш /x/c очищен", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, CommonSettings.class));
        } else if (id == R.id.nav_help) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
