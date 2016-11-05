package vit01.idecmobile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.notify.workerJob;

public class MainActivity extends AppCompatActivity {
    private static final int ADD_NODE = 100000;
    private static final int MANAGE_NODE = 100001;
    public Station currentStation;
    public EcholistFragment echolist;
    public int currentStationIndex = 0;
    public boolean is_offline_list_now = false;
    public Drawer drawer;
    public AccountHeader drawerHeader;
    public SharedPreferences sharedPref;
    public SharedPreferences.Editor prefEditor;
    public AbstractTransport transport;
    FragmentManager fm;
    int MY_PERMISSION_WRITE_STORAGE;
    SwipeRefreshLayout swipeRefresh;
    OnSwipeTouchListener swipeDrawerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Config.loadConfig(this);
        setTheme(Config.appTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_fetch);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Intent intent = new Intent(MainActivity.this, DebugActivity.class);
                intent.putExtra("task", "fetch");
                startActivity(intent);

                swipeRefresh.setRefreshing(false);
            }
        });

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(getHeaderDrawable())
                .withCompactStyle(true)
                .withProfileImagesVisible(true)
                .withProfileImagesClickable(false)
                .withNameTypeface(Typeface.DEFAULT_BOLD)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        if (profile instanceof IDrawerItem) {
                            long identifier = profile.getIdentifier();
                            int countStations = Config.values.stations.size();

                            if (identifier == ADD_NODE) {
                                Config.values.stations.add(new Station());
                                Intent editNew = new Intent(MainActivity.this, StationsActivity.class);
                                editNew.putExtra("index", countStations);
                                startActivity(editNew);
                            } else if (identifier == MANAGE_NODE) {
                                Intent manageIntent = new Intent(MainActivity.this, StationsActivity.class);
                                manageIntent.putExtra("index", currentStationIndex);
                                startActivity(manageIntent);
                            } else if (identifier < countStations) {
                                // Выбрали другую станцию, обновляем список
                                currentStationIndex = (int) identifier;
                                currentStation = Config.values.stations.get(currentStationIndex);
                                saveCurrentStationPosition(currentStationIndex);
                                updateEcholist();
                                return true;
                            }
                            return true;
                        }
                        return false;
                    }
                })
                .build();

        PrimaryDrawerItem echoItem = new PrimaryDrawerItem().withIdentifier(1).withName("Эхоконференции").withIcon(GoogleMaterial.Icon.gmd_message);
        PrimaryDrawerItem carbonItem = new PrimaryDrawerItem().withIdentifier(2).withName("Карбонка").withIcon(GoogleMaterial.Icon.gmd_input).withSelectable(false);
        PrimaryDrawerItem unreadItem = new PrimaryDrawerItem().withIdentifier(3).withName("Непрочитанные").withIcon(GoogleMaterial.Icon.gmd_remove_red_eye).withSelectable(false);
        PrimaryDrawerItem sentItem = new PrimaryDrawerItem().withIdentifier(4).withName("Отправленные").withIcon(GoogleMaterial.Icon.gmd_send).withSelectable(false);
        final PrimaryDrawerItem draftsItem = new PrimaryDrawerItem().withIdentifier(5).withName("Черновики").withIcon(GoogleMaterial.Icon.gmd_drafts).withSelectable(false);
        PrimaryDrawerItem starredItem = new PrimaryDrawerItem().withIdentifier(6).withName("Избранные").withIcon(GoogleMaterial.Icon.gmd_star).withSelectable(false);
        final PrimaryDrawerItem offlineItem = new PrimaryDrawerItem().withIdentifier(7).withName("Offline-эхи").withIcon(GoogleMaterial.Icon.gmd_signal_wifi_off);
        PrimaryDrawerItem extItem = new PrimaryDrawerItem().withIdentifier(8).withName("Дополнительно").withIcon(GoogleMaterial.Icon.gmd_extension).withSelectable(false);
        PrimaryDrawerItem settingsItem = new PrimaryDrawerItem().withIdentifier(9).withName("Настройки").withIcon(GoogleMaterial.Icon.gmd_settings).withSelectable(false);
        PrimaryDrawerItem helpItem = new PrimaryDrawerItem().withIdentifier(10).withName("Помощь").withIcon(GoogleMaterial.Icon.gmd_help).withSelectable(false);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(drawerHeader)
                .withToolbar(toolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(false)
                .addDrawerItems(
                        echoItem,
                        carbonItem,
                        unreadItem,
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
                        if (drawerItem != null) switch ((int) drawerItem.getIdentifier()) {
                            case 1:
                                if (is_offline_list_now) {
                                    is_offline_list_now = false;
                                    updateEcholist();
                                }
                                break;
                            case 2:
                                Intent intent = new Intent(MainActivity.this, EchoView.class);
                                intent.putExtra("echoarea", "_carbon_classic");
                                intent.putExtra("nodeindex", -1);
                                startActivity(intent);
                                break;
                            case 3:
                                ArrayList<String> unread = transport.getAllUnreadMessages();

                                if (unread.size() == 0) {
                                    Toast.makeText(MainActivity.this, "Непрочитанных сообщений нет!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent unreadIntent = new Intent(MainActivity.this, MessageSlideActivity.class);
                                    unreadIntent.putExtra("msglist", unread);
                                    unreadIntent.putExtra("position", 0);
                                    startActivity(unreadIntent);
                                }
                                break;
                            case 4:
                                Intent sent = new Intent(MainActivity.this, DraftsView.class);
                                sent.putExtra("unsent", false);
                                startActivity(sent);
                                break;
                            case 5:
                                Intent unsent = new Intent(MainActivity.this, DraftsView.class);
                                unsent.putExtra("unsent", true);
                                startActivity(unsent);
                                break;
                            case 6:
                                Intent favorites = new Intent(MainActivity.this, EchoView.class);
                                favorites.putExtra("echoarea", "_favorites");
                                favorites.putExtra("nodeindex", -1);
                                startActivity(favorites);
                                break;
                            case 7:
                                if (!is_offline_list_now) {
                                    is_offline_list_now = true;
                                    updateEcholist();
                                }
                                break;
                            case 8:
                                startActivity(new Intent(MainActivity.this, AdditionalActivity.class));
                                break;
                            case 9:
                                startActivity(new Intent(MainActivity.this, CommonSettings.class));
                                break;
                            case 10:
                                startActivity(new Intent(MainActivity.this, HelpActivity.class));
                                break;
                        }
                        return false;
                    }
                })
                .build();

        GlobalTransport.transport = new SqliteTransport(this);
        transport = GlobalTransport.transport;
        updateStationsList();

        echolist = EcholistFragment.newInstance(currentStation.echoareas, currentStationIndex);

        fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(swipeRefresh.getId(), echolist)
                .commit();

        is_offline_list_now = false;
        updateNavDrawerCounters();

        if (Config.values.firstRun) {
            Config.values.firstRun = false;
            startActivity(new Intent(this, StationsActivity.class));
        }

        String task = getIntent().getStringExtra("task");

        if (task != null && task.equals("fetch")) {
            Intent fetcher = new Intent(MainActivity.this, DebugActivity.class);
            fetcher.putExtra("task", "fetch");
            startActivity(fetcher);

            workerJob.lastDifference = null; // чистим данные уведомлений, сбрасывая счётчик непрочитанных
            setIntent(new Intent()); // убиваем это дело, чтобы после поворота экрана снова не запустился фетчер
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_WRITE_STORAGE);
        }

        boolean tor_is_fine = SimpleFunctions.checkTorRunning(this, true);
        if (!tor_is_fine) {
            Intent showStart = OrbotHelper.getShowOrbotStartIntent();
            startActivity(showStart);
        }

        swipeDrawerListener = new OnSwipeTouchListener(this) {
            @Override
            public boolean onSwipeRight() {
                if (!drawer.isDrawerOpen()) {
                    drawer.openDrawer();
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean status = swipeDrawerListener.gestureDetector.onTouchEvent(ev);
        return (status || super.dispatchTouchEvent(ev));
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStationsList();
        updateEcholist();
        updateNavDrawerCounters();
        swipeRefresh.setEnabled(Config.values.swipeToFetch);
    }

    public void updateStationsList() {
        // добавляем станции в navigation drawer

        drawerHeader.clear();

        for (int i = 0; i < Config.values.stations.size(); i++) {
            Station station = Config.values.stations.get(i);
            String nodename = station.nodename;

            drawerHeader.addProfile(new ProfileDrawerItem()
                    .withName(nodename)
                    .withIcon(getStationIcon(nodename))
                    .withIdentifier(i), i);
        }

        drawerHeader.addProfiles(
                new ProfileSettingDrawerItem().withName("Добавить станцию").withIdentifier(ADD_NODE).withIcon(GoogleMaterial.Icon.gmd_add),
                new ProfileSettingDrawerItem().withName("Управление станциями").withIdentifier(MANAGE_NODE).withIcon(GoogleMaterial.Icon.gmd_settings)
        );

        currentStationIndex = sharedPref.getInt("nodeindex_current", 0);

        if (currentStationIndex > Config.values.stations.size() - 1) {
            currentStationIndex = 0;
            saveCurrentStationPosition(0);
        }
        currentStation = Config.values.stations.get(currentStationIndex);
        drawerHeader.setActiveProfile(currentStationIndex);
    }

    public void updateEcholist() {
        if (!is_offline_list_now)
            echolist.updateState(currentStation.echoareas, currentStationIndex);
        else
            echolist.updateState(Config.values.offlineEchoareas, -1);
    }

    public void updateNavDrawerCounters() {
        int countUnread = transport.countUnread();
        int countFavorites = transport.countFavorites();

        drawer.updateBadge(3, new StringHolder(String.valueOf(countUnread)));
        drawer.updateBadge(6, new StringHolder(String.valueOf(countFavorites)));
    }

    public void saveCurrentStationPosition(int position) {
        prefEditor = sharedPref.edit();
        prefEditor.putInt("nodeindex_current", position);
        prefEditor.apply();
    }

    public Drawable getStationIcon(String stationName) {
        int hash = stationName.hashCode();
        int colors[] = new int[3];

        colors[0] = (hash & 0xFF0000) >> 16;
        colors[1] = (hash & 0x00FF00) >> 8;
        colors[2] = (hash & 0x0000FF);

        for (int i = 0; i < 3; i++) if (colors[i] > 200) colors[i] -= 50;

        Drawable icon = getResources().getDrawable(R.drawable.ic_station);
        assert icon != null;
        icon.mutate().setColorFilter(Color.rgb(colors[0], colors[1], colors[2]), PorterDuff.Mode.OVERLAY);
        return icon;
    }

    public Drawable getHeaderDrawable() {
        TypedValue typedValue = new TypedValue();
        TypedArray a = obtainStyledAttributes(typedValue.data, new int[]{R.attr.navdrawer_picture});
        Drawable drawable = a.getDrawable(0);
        a.recycle();
        return drawable;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        menu.findItem(R.id.action_fetch).setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_get_app)
                        .actionBar().color(iconColor));

        menu.findItem(R.id.action_send).setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_cloud_upload)
                        .actionBar().color(iconColor));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, CommonSettings.class));
            return true;
        } else if (id == R.id.action_fetch) {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra("task", "fetch");
            startActivity(intent);
            return true;
        } else if (id == R.id.action_send) {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra("task", "send");
            startActivity(intent);
            return true;
        } else if (id == R.id.action_stations) {
            Intent manageIntent = new Intent(MainActivity.this, StationsActivity.class);
            manageIntent.putExtra("index", currentStationIndex);
            startActivity(manageIntent);
            return true;
        } else if (id == R.id.action_mark_all_base_read) {
            transport.setUnread(false, (String) null);
            updateEcholist();
        }

        return super.onOptionsItemSelected(item);
    }
}