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
import android.view.MenuItem;

import java.util.ArrayList;

import vit01.idecmobile.Core.AbstractTransport;
import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.Core.SqliteTransport;

public class MessageSlideActivity extends AppCompatActivity {
    ActionBar actionBar;
    ViewPager mPager;
    private int msgCount;
    private ArrayList<String> msglist;
    private ArrayList<String> user_watched_these = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        SimpleFunctions.resetIDECParserColors();

        Intent gotInfo = getIntent();
        msglist = gotInfo.getStringArrayListExtra("msglist");
        msgCount = msglist.size();
        int firstPosition = gotInfo.getIntExtra("position", msgCount - 1);

        mPager = (ViewPager) findViewById(R.id.swipe_pager);
        PagerAdapter pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(firstPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateActionBar(position);
                String msgid = msglist.get(position);

                // добавляем сообщение в прочитанные
                if (!user_watched_these.contains(msgid)) {
                    user_watched_these.add(msgid);
                }
            }
        });

        user_watched_these.add(msglist.get(firstPosition));
        updateActionBar(firstPosition);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Помечаем нужные сообщения прочитанными
        AbstractTransport transport = new SqliteTransport(getApplicationContext());
        transport.setUnread(false, user_watched_these);
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
