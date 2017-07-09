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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.ArrayList;

import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.prefs.Config;

public class MessageSlideActivity extends AppCompatActivity {
    private String echoarea = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);

        if (SimpleFunctions.isTablet(this)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_message_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (Config.values.hide_toolbar_when_scrolling) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }

        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);

        SimpleFunctions.resetIDECParserColors();

        Intent gotInfo = getIntent();
        ArrayList<String> msglist = gotInfo.getStringArrayListExtra("msglist");

        if (msglist == null || msglist.size() == 0) {
            Toast.makeText(MessageSlideActivity.this, R.string.empty_msglist, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int nodeIndex = gotInfo.getIntExtra("nodeindex", Config.currentSelectedStation);

        int firstPosition = gotInfo.getIntExtra("position", msglist.size() - 1);
        if (gotInfo.hasExtra("echoarea")) echoarea = gotInfo.getStringExtra("echoarea");

        ((MessageSlideFragment) getSupportFragmentManager().findFragmentById(R.id.messages_slider))
                .initSlider(echoarea, msglist, nodeIndex, firstPosition);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        onPause();
        if (Config.values.disableMsglist) {
            setResult(1);
        } else setResult(0); // return to list
        super.onBackPressed();
    }
}