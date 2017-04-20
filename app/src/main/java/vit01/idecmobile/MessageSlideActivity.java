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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;
import java.util.Collections;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.EchoReadingPosition;
import vit01.idecmobile.Core.GlobalTransport;
import vit01.idecmobile.Core.IIMessage;
import vit01.idecmobile.Core.SimpleFunctions;

public class MessageSlideActivity extends AppCompatActivity {
    ActionBar actionBar;
    ViewPager mPager;
    private int msgCount;
    private int nodeIndex;
    private ArrayList<String> msglist;
    private String echoarea = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);

        SimpleFunctions.resetIDECParserColors();

        Intent gotInfo = getIntent();
        msglist = gotInfo.getStringArrayListExtra("msglist");
        nodeIndex = gotInfo.getIntExtra("nodeindex", Config.currentSelectedStation);
        msgCount = msglist.size();
        int firstPosition = gotInfo.getIntExtra("position", msgCount - 1);
        if (gotInfo.hasExtra("echoarea")) echoarea = gotInfo.getStringExtra("echoarea");

        mPager = (ViewPager) findViewById(R.id.swipe_pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(firstPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateActionBar(position);
                if (Config.values.disableMsglist && echoarea != null)
                    EchoReadingPosition.setPosition(echoarea, position);
                final String msgid = msglist.get(position);

                // Помечаем сообщение прочитанным
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GlobalTransport.transport.setUnread(false, Collections.singletonList(msgid));
                    }
                }).start();
            }
        });

        // помечаем прочитанным первое сообщение
        GlobalTransport.transport.setUnread(false, Collections.singletonList(msglist.get(firstPosition)));
        updateActionBar(firstPosition);
        if (Config.values.disableMsglist && echoarea != null) {
            EchoReadingPosition.setPosition(echoarea, firstPosition);
        }
    }

    public void updateActionBar(int position) {
        actionBar.setTitle(String.valueOf(position + 1) + " из " + msgCount);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_first_item:
                mPager.setCurrentItem(0, false);
                return true;
            case R.id.action_last_item:
                mPager.setCurrentItem(msgCount - 1, false);
                return true;
            case R.id.action_new_message:
                IIMessage msg = GlobalTransport.transport.getMessage
                        (msglist.get(mPager.getCurrentItem()));

                Intent intent = new Intent(MessageSlideActivity.this, DraftEditor.class);
                intent.putExtra("task", "new_in_echo");
                intent.putExtra("echoarea", msg.echo);
                intent.putExtra("nodeindex", nodeIndex);
                startActivity(intent);
                return true;
            case R.id.action_msglist_return:
                EchoReadingPosition.writePositionCache();
                setResult(0);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (Config.values.disableMsglist && echoarea != null) {
            EchoReadingPosition.writePositionCache();
            setResult(1);
        } else setResult(0); // return to list
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        if (Config.values.disableMsglist && echoarea != null)
            EchoReadingPosition.writePositionCache();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_view, menu);
        MenuItem return_msglist = menu.findItem(R.id.action_msglist_return);

        int iconColor = SimpleFunctions.colorFromTheme(this, R.attr.menuIconColor);

        if (Config.values.disableMsglist && echoarea != null) {
            return_msglist.setVisible(true);
            return_msglist.setIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_format_list_bulleted).actionBar().color(iconColor));
        }

        MenuItem tostart = menu.findItem(R.id.action_first_item);
        MenuItem toend = menu.findItem(R.id.action_last_item);

        if (msgCount == 1) {
            tostart.setVisible(false);
            toend.setVisible(false);
        } else {
            IconicsDrawable startIcon = new IconicsDrawable(this, GoogleMaterial.Icon.gmd_keyboard_arrow_left).actionBar().color(iconColor);
            IconicsDrawable endIcon = new IconicsDrawable(this, GoogleMaterial.Icon.gmd_keyboard_arrow_right).actionBar().color(iconColor);

            tostart.setIcon(startIcon);
            toend.setIcon(endIcon);
        }

        IconicsDrawable newMsg = new IconicsDrawable(this, GoogleMaterial.Icon.gmd_create).actionBar().color(iconColor);
        MenuItem compose = menu.findItem(R.id.action_new_message);
        compose.setIcon(newMsg);

        return super.onCreateOptionsMenu(menu);
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return MessageView_full.newInstance(msglist, position);
        }

        @Override
        public int getCount() {
            return msgCount;
        }
    }
}