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

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.notify.workerJob;

public class MainActivity extends AppCompatActivity {
    private static final int ADD_NODE = 100000;
    private static final int MANAGE_NODE = 100001;
    public Station currentStation;
    public EcholistFragment echolist;
    public boolean is_offline_list_now = false;
    public Drawer drawer;
    public AccountHeader drawerHeader;
    public AbstractTransport transport;
    FragmentManager fm;
    int MY_PERMISSION_WRITE_STORAGE;
    SwipeRefreshLayout swipeRefresh;
    OnSwipeTouchListener swipeDrawerListener;
    SearchAdvancedFragment advsearch;
    MenuItem fetchItem, sendItem, advancedSearchItem;
    SearchView searchView;

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

        Config.getCurrentStationPosition(this);

        drawerHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(getHeaderDrawable())
                .withCompactStyle(true)
                .withProfileImagesVisible(false)
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
                                manageIntent.putExtra("index", Config.currentSelectedStation);
                                startActivity(manageIntent);
                            } else if (identifier < countStations) {
                                // Выбрали другую станцию, обновляем список
                                Config.currentSelectedStation = (int) identifier;
                                currentStation = Config.values.stations.get(Config.currentSelectedStation);
                                Config.saveCurrentStationPosition();
                                updateEcholist();
                                return true;
                            }
                            return true;
                        }
                        return false;
                    }
                })
                .build();

        int shadowColor = SimpleFunctions.colorFromTheme(this, R.attr.material_drawer_header_text_shadow);
        TextView headerText = (TextView) drawerHeader.getView().findViewById(R.id.material_drawer_account_header_name);
        TextView emailText = (TextView) drawerHeader.getView().findViewById(R.id.material_drawer_account_header_email);
        headerText.setShadowLayer(5, 2, 2, shadowColor);
        emailText.setShadowLayer(5, 2, 2, shadowColor);
        emailText.setTextSize(15f);
        emailText.setAlpha(0.8f);

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
        PrimaryDrawerItem updateItem = new PrimaryDrawerItem().withIdentifier(11).withName("Обновиться").withIcon(GoogleMaterial.Icon.gmd_system_update).withSelectable(false);
        PrimaryDrawerItem infoItem = new PrimaryDrawerItem().withIdentifier(12).withName("Дата сборки").withIcon(GoogleMaterial.Icon.gmd_info).withSelectable(false);

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
                        helpItem,
                        updateItem,
                        infoItem
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
                            case 11:
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://ii-net.tk/ii/files/app-debug.apk")));
                                break;
                            case 12:
                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(BuildConfig.VERSION_NAME)
                                        .setPositiveButton("ОК", null)
                                        .show();
                                break;
                        }
                        return false;
                    }
                })
                .build();

        transport = GlobalTransport.transport(this);
        updateStationsList();

        echolist = EcholistFragment.newInstance(currentStation.echoareas, Config.currentSelectedStation);

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_WRITE_STORAGE);
        }

        boolean tor_is_fine = SimpleFunctions.checkTorRunning(this, true);
        if (!tor_is_fine) {
            Intent showStart = OrbotHelper.getShowOrbotStartIntent();
            startActivity(showStart);
        }

        swipeDrawerListener = new OnSwipeTouchListener(this, echolist) {
            @Override
            public boolean onSwipeRight() {
                if (!drawer.isDrawerOpen()) {
                    drawer.openDrawer();
                    return true;
                }
                return false;
            }
        };

        advsearch = SearchAdvancedFragment.newInstance();
        ExternalStorage.initStorage();
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
        Config.getCurrentStationPosition(this);
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

            ProfileDrawerItem dritem = new ProfileDrawerItem()
                    .withEmail(station.address)
                    .withName(nodename)
                    .withNameShown(true)
                    .withIcon(getStationIcon(nodename))
                    .withIdentifier(i);

            drawerHeader.addProfile(dritem, i);
        }

        drawerHeader.addProfiles(
                new ProfileSettingDrawerItem().withName("Добавить станцию").withIdentifier(ADD_NODE).withIcon(GoogleMaterial.Icon.gmd_add),
                new ProfileSettingDrawerItem().withName("Управление станциями").withIdentifier(MANAGE_NODE).withIcon(GoogleMaterial.Icon.gmd_settings)
        );

        if (Config.currentSelectedStation > (Config.values.stations.size() - 1)) {
            Config.currentSelectedStation = 0;
            Config.saveCurrentStationPosition();
        }

        currentStation = Config.values.stations.get(Config.currentSelectedStation);
        drawerHeader.setActiveProfile(Config.currentSelectedStation);
    }

    public void updateEcholist() {
        if (!is_offline_list_now)
            echolist.updateState(currentStation.echoareas, Config.currentSelectedStation);
        else
            echolist.updateState(Config.values.offlineEchoareas, -1);
    }

    public void updateNavDrawerCounters() {
        int countUnread = transport.countUnread();
        int countFavorites = transport.countFavorites();

        drawer.updateBadge(3, new StringHolder(String.valueOf(countUnread)));
        drawer.updateBadge(6, new StringHolder(String.valueOf(countFavorites)));
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

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                triggerSearch(null, new Bundle());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        fetchItem = menu.findItem(R.id.action_fetch);
        sendItem = menu.findItem(R.id.action_send);
        advancedSearchItem = menu.findItem(R.id.action_advancedsearch);

        fetchItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_get_app)
                        .actionBar().color(iconColor));

        sendItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_cloud_upload)
                        .actionBar().color(iconColor));

        searchItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_search)
                        .actionBar().color(iconColor));

        advancedSearchItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_expand_more)
                        .actionBar().color(iconColor));

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                fetchItem.setVisible(false);
                sendItem.setVisible(false);
                advancedSearchItem.setVisible(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                advancedSearchItem.setVisible(false);
                fetchItem.setVisible(true);
                sendItem.setVisible(true);
                invalidateOptionsMenu();
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, CommonSettings.class));
                return true;
            case R.id.action_fetch: {
                Intent intent = new Intent(this, DebugActivity.class);
                intent.putExtra("task", "fetch");
                startActivity(intent);
                return true;
            }
            case R.id.action_send: {
                Intent intent = new Intent(this, DebugActivity.class);
                intent.putExtra("task", "send");
                startActivity(intent);
                return true;
            }
            case R.id.action_stations:
                Intent manageIntent = new Intent(MainActivity.this, StationsActivity.class);
                manageIntent.putExtra("index", Config.currentSelectedStation);
                startActivity(manageIntent);
                return true;
            case R.id.action_mark_all_base_read:
                transport.setUnread(false, (String) null);
                updateEcholist();
                return true;
            case R.id.action_search:
                if (!item.isActionViewExpanded()) item.expandActionView();
                return true;
            case R.id.action_advancedsearch:
                advsearch.show(fm, advsearch.getTag());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            setContentView(R.layout.content_grant_permission);
            ImageView v = (ImageView) findViewById(R.id.imageView);

            int secondaryText = SimpleFunctions.colorFromTheme(this, android.R.attr.textColorSecondary);
            IconicsDrawable icon = new IconicsDrawable(this, GoogleMaterial.Icon.gmd_warning).color(secondaryText);
            v.setImageDrawable(icon);
        }
    }

    @Override
    public void triggerSearch(String initialQuery, Bundle bundle) {
        String query = searchView.getQuery().toString();

        if (query.equals("") || TextUtils.isEmpty(query)) initialQuery = "___query_empty";
        else initialQuery = query;

        bundle.putAll(advsearch.getDataBundle());

        if (Build.VERSION.SDK_INT < 21) {
            Intent searchIntent = new Intent(this, SearchActivity.class);
            searchIntent.setAction(Intent.ACTION_SEARCH);
            searchIntent.putExtra(SearchManager.QUERY, query);
            searchIntent.putExtra(SearchManager.APP_DATA, bundle);
            startActivity(searchIntent);
        } else {
            super.triggerSearch(initialQuery, bundle);
        }
    }
}