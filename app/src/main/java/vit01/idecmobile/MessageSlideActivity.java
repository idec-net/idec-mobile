package vit01.idecmobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;

public class MessageSlideActivity extends AppCompatActivity implements MessageView_full.OnFragmentInteractionListener {
    private int msgCount, firstPosition;
    private ArrayList<String> msglist;
    private ViewPager mPager;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gotInfo = getIntent();
        msglist = gotInfo.getStringArrayListExtra("msglist");
        msgCount = msglist.size();
        firstPosition = gotInfo.getIntExtra("position", msgCount - 1);

        mPager = (ViewPager) findViewById(R.id.swipe_pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(pagerAdapter);
        mPager.setCurrentItem(firstPosition);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // now do nothing
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
