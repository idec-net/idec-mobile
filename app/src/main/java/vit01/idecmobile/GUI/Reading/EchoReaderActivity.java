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

package vit01.idecmobile.GUI.Reading;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;

import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;
import vit01.idecmobile.SearchActivity;
import vit01.idecmobile.SearchAdvancedFragment;
import vit01.idecmobile.prefs.Config;

public class EchoReaderActivity extends AppCompatActivity {
    SearchAdvancedFragment advsearch;
    MenuItem advancedSearchItem;
    SearchView searchView;
    String echoarea;
    ArrayList<String> msgids;
    int nodeIndex;
    MessageListFragment listFragment;

    boolean isTablet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        isTablet = SimpleFunctions.isTablet(this);
        setContentView(R.layout.activity_reader);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SimpleFunctions.setDisplayHomeAsUpEnabled(this);

        Intent intent = getIntent();

        echoarea = intent.getStringExtra("echoarea");
        msgids = intent.getStringArrayListExtra("msglist");
        nodeIndex = intent.getIntExtra("nodeindex", -1);

        if (nodeIndex < 0) {
            findViewById(R.id.fab).setVisibility(View.INVISIBLE);
        }

        SimpleFunctions.setActivityTitle(this, IDECFunctions.getAreaName(echoarea));
        advsearch = SearchAdvancedFragment.newInstance(echoarea);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MessageListFragment.alreadyOpenedSliderActivity = true;
        if (requestCode == 1) {
            if (resultCode == 1 && !SimpleFunctions.isTablet(this)) finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        listFragment = (MessageListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.msglist);
        listFragment.initEchoView(echoarea, msgids, nodeIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_echoview, menu);

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

        advancedSearchItem = menu.findItem(R.id.action_advancedsearch);

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        searchItem.setIcon(
                new IconicsDrawable(this, GoogleMaterial.Icon.gmd_search)
                        .actionBar().color(iconColor));

        advancedSearchItem.setIcon(
                new IconicsDrawable(this, GoogleMaterial.Icon.gmd_expand_more)
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
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                if (!item.isActionViewExpanded()) item.expandActionView();
                return true;
            case R.id.action_advancedsearch:
                advsearch.show(getSupportFragmentManager(), advsearch.getTag());
                return true;
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