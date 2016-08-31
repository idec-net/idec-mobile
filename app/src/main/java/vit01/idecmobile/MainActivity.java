package vit01.idecmobile;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public Station currentStation;
    public ListView echoList;
    public ArrayAdapter echoListAdapter;
    public int currentStationIndex = 0;
    public Drawer drawer;

    private static final int ADD_NODE = 100000;
    private static final int MANAGE_NODE = 100001;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Config.loadConfig(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final List<String> stationsList = new ArrayList<>();

        for (Station station : Config.values.stations) {
            stationsList.add(station.nodename);
        }

        AccountHeader drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withCompactStyle(true)
                .withProfileImagesVisible(false)
                .addProfiles(
                        new ProfileSettingDrawerItem().withName("Добавить станцию").withIdentifier(ADD_NODE).withIcon(GoogleMaterial.Icon.gmd_add),
                        new ProfileSettingDrawerItem().withName("Управление станциями").withIdentifier(MANAGE_NODE).withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == ADD_NODE) {
                            Toast.makeText(MainActivity.this, "Диалог добавления станции не реализован", Toast.LENGTH_SHORT).show();
                        }

                        // Выбрали другую станцию, обновляем список
                        if (profile instanceof IDrawerItem && profile.getIdentifier() < stationsList.size()) {
                            currentStation = Config.values.stations.get((int)profile.getIdentifier());
                            currentStationIndex = (int)profile.getIdentifier();
                            updateEcholist();
                        }
                        return false;
                    }
                })
                .build();

        // добавляем станции в navigation drawer
        for (int i = 0; i < stationsList.size(); i++) {
            drawerHeader.addProfile(new ProfileDrawerItem().withEmail(stationsList.get(i)).withIdentifier(i), i);
        }

        PrimaryDrawerItem echoItem = new PrimaryDrawerItem().withIdentifier(1).withName("Эхоконференции").withIcon(GoogleMaterial.Icon.gmd_message);
        PrimaryDrawerItem carbonItem = new PrimaryDrawerItem().withIdentifier(2).withName("Карбонка").withIcon(GoogleMaterial.Icon.gmd_input);
        PrimaryDrawerItem sentItem = new PrimaryDrawerItem().withIdentifier(3).withName("Отправленные").withIcon(GoogleMaterial.Icon.gmd_send);
        PrimaryDrawerItem draftsItem = new PrimaryDrawerItem().withIdentifier(4).withName("Черновики").withIcon(GoogleMaterial.Icon.gmd_drafts);
        PrimaryDrawerItem starredItem = new PrimaryDrawerItem().withIdentifier(5).withName("Избранные").withIcon(GoogleMaterial.Icon.gmd_star);
        PrimaryDrawerItem offlineItem = new PrimaryDrawerItem().withIdentifier(6).withName("Offline-эхи").withIcon(GoogleMaterial.Icon.gmd_signal_wifi_off);
        PrimaryDrawerItem extItem = new PrimaryDrawerItem().withIdentifier(7).withName("Дополнительно").withIcon(GoogleMaterial.Icon.gmd_extension);
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem().withIdentifier(8).withName("Настройки").withIcon(GoogleMaterial.Icon.gmd_settings).withSelectable(false);
        PrimaryDrawerItem helpItem = new PrimaryDrawerItem().withIdentifier(9).withName("Помощь").withIcon(GoogleMaterial.Icon.gmd_help).withSelectable(false);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(drawerHeader)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(
                        echoItem,
                        carbonItem,
                        sentItem,
                        draftsItem,
                        starredItem,
                        offlineItem,
                        extItem,
                        new DividerDrawerItem(),
                        settingsItem,
                        helpItem
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                           if (drawerItem.getIdentifier() == 8) {
                                startActivity(new Intent(MainActivity.this, CommonSettings.class));
                            } // else if () {}
                        }
                        return false;
                    }
                })
                .build();

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
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
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
        } else if (id == R.id.action_delete_everything) {
            SqliteTransport transport = new SqliteTransport(this);
            transport.FuckDeleteEverything();
            Toast.makeText(MainActivity.this, "База очищена", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_stations) {
            startActivity(new Intent(this, StationsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
