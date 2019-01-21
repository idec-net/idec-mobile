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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;

import vit01.idecmobile.Core.ExternalStorage;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.Station;
import vit01.idecmobile.ListEditActivity;
import vit01.idecmobile.R;
import vit01.idecmobile.SearchActivity;
import vit01.idecmobile.SearchAdvancedFragment;
import vit01.idecmobile.gui_helpers.OnSwipeTouchListener;
import vit01.idecmobile.prefs.Config;

public class FilesActivity extends AppCompatActivity {
    public Station currentStation;
    public Drawer drawer;
    FragmentManager fm;
    OnSwipeTouchListener swipeDrawerListener;
    SearchAdvancedFragment advsearch;
    MenuItem advancedSearchItem;
    SearchView searchView;
    FileListFragment filelist;
    int nodeindex;

    int selectedEcho = 0;
    boolean shouldOpenDrawer = true;
    Bundle savedInstance;

    String sortOptions[] = new String[]{"number desc", "number asc", "filename desc", "filename asc",
            "serversize desc", "serversize asc"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean isTablet = SimpleFunctions.isTablet(this);

        Intent gotIntent = getIntent();

        nodeindex = gotIntent.getIntExtra("nodeindex", 0);
        currentStation = Config.values.stations.get(nodeindex);

        if (savedInstanceState != null) {
            selectedEcho = savedInstanceState.getInt("selectedEcho");
            if (selectedEcho >= currentStation.file_echoareas.size()) selectedEcho = 0;

            shouldOpenDrawer = savedInstanceState.getBoolean("openDrawer");
            savedInstance = savedInstanceState;
        }

        ArrayList<IDrawerItem> drawerAreas = new ArrayList<>();

        PrimaryDrawerItem editItem = new PrimaryDrawerItem().withIdentifier(Integer.MAX_VALUE).withName(R.string.title_activity_list_edit)
                .withIcon(GoogleMaterial.Icon.gmd_edit).withSelectable(false);
        drawerAreas.add(editItem);
        drawerAreas.add(new DividerDrawerItem());

        for (int i = 0; i < currentStation.file_echoareas.size(); i++) {
            String fecho = currentStation.file_echoareas.get(i);
            PrimaryDrawerItem fecho_item = new PrimaryDrawerItem().withIdentifier(i)
                    .withName(fecho).withSelectable(true)
                    .withBadge(String.valueOf(GlobalTransport.transport.countFiles(fecho)));
            drawerAreas.add(fecho_item);
        }

        DrawerBuilder drawerBuilder = new DrawerBuilder()
                .withActivity(this)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(false)
                .withDrawerItems(drawerAreas)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            int itemId = (int) drawerItem.getIdentifier();

                            if (itemId == Integer.MAX_VALUE) {
                                Intent intent = new Intent(FilesActivity.this, ListEditActivity.class);
                                intent.putExtra("type", "fromstation-fecho");
                                intent.putExtra("index", nodeindex);
                                finish();
                                FilesActivity.this.startActivity(intent);
                                return false;
                            }

                            String fecho = currentStation.file_echoareas.get(itemId);
                            String number = ((PrimaryDrawerItem) drawerItem).getBadge().getText().toString();

                            SimpleFunctions.setActivityTitle(FilesActivity.this, fecho + " (" + number + ")");
                            filelist.initFEchoView(fecho, null, nodeindex, sortOptions[Config.values.fecho_sort_type]);
                            selectedEcho = itemId;
                        }
                        return false;
                    }
                });

        if (isTablet) {
            drawer = drawerBuilder.buildView();
            ((ViewGroup) findViewById(R.id.drawer_holder)).addView(drawer.getSlider());
        } else {
            drawer = drawerBuilder.withToolbar(toolbar).build();
        }

        // advsearch = SearchAdvancedFragment.newInstance();
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("openDrawer", false);
        outState.putInt("selectedEcho", selectedEcho);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        String firstFecho = currentStation.file_echoareas.get(selectedEcho);
        filelist = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.filelist_fragment);
        filelist.initFEchoView(firstFecho, null, nodeindex, sortOptions[Config.values.fecho_sort_type]);

        SimpleFunctions.setActivityTitle(this, firstFecho + " (" + GlobalTransport.transport.countFiles(firstFecho) + ")");

        swipeDrawerListener = new OnSwipeTouchListener(this, filelist) {
            @Override
            public boolean onSwipeRight() {
                if (!drawer.isDrawerOpen()) {
                    drawer.openDrawer();
                    return true;
                }
                return false;
            }
        };
        drawer.setSelection(selectedEcho);
        if (shouldOpenDrawer) {
            drawer.openDrawer();
            shouldOpenDrawer = false;
        }
    }

    @Override
    protected void onPause() {
        if (savedInstance != null) onSaveInstanceState(savedInstance);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fechoes, menu);

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        MenuItem sortItem = menu.findItem(R.id.sorting);
        sortItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_sort)
                        .actionBar().color(iconColor));

        // TODO: make search reality

        /*SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
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

        advancedSearchItem = menu.findItem(R.id.action_advancedsearch);

        searchItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_search)
                        .actionBar().color(iconColor));

        advancedSearchItem.setIcon(
                new IconicsDrawable(getApplicationContext(), GoogleMaterial.Icon.gmd_expand_more)
                        .actionBar().color(iconColor));

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                advancedSearchItem.setVisible(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                advancedSearchItem.setVisible(false);
                invalidateOptionsMenu();
                return true;
            }
        });*/

        return true;
    }

    public int returnSortOrder(int id) {
        switch (id) {
            case R.id.sort_date_desc:
                return 0;
            case R.id.sort_date_asc:
                return 1;
            case R.id.sort_name_desc:
                return 2;
            case R.id.sort_name_asc:
                return 3;
            case R.id.sort_size_desc:
                return 4;
            case R.id.sort_size_asc:
                return 5;
        }
        return 1;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                if (!item.isActionViewExpanded()) item.expandActionView();
                return true;
            case R.id.action_advancedsearch:
                advsearch.show(fm, advsearch.getTag());
                return true;
            case R.id.sorting:
                return true;
            default:
                Config.values.fecho_sort_type = returnSortOrder(id);
                item.setChecked(true);
                String fecho = currentStation.file_echoareas.get(selectedEcho);
                filelist.initFEchoView(fecho, null, nodeindex, sortOptions[Config.values.fecho_sort_type]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Config.writeConfig(getApplicationContext());
                    }
                }).start();
        }

        return super.onOptionsItemSelected(item);
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