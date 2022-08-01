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

package vit01.idecmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.io.InputStream;

import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.prefs.Config;

public class HelperActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, getString(R.string.title_activity_help));

        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        Intent gotIntent = getIntent();
        String selectedTab = gotIntent.hasExtra("tab") ? gotIntent.getStringExtra("tab") : "";
        int whichTab = 0;

        switch (selectedTab) {
            case "about":
                whichTab = 0;
                break;
            case "newbie":
                whichTab = 1;
                break;
            case "license":
                whichTab = 2;
                break;
        }

        mViewPager.setCurrentItem(whichTab);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class About_Us extends Fragment {
        public About_Us() {
        }

        public static About_Us newInstance() {
            return new About_Us();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.content_about, container, false);
        }
    }

    public static class Newbie_guide extends Fragment {
        public Newbie_guide() {
        }

        public static Newbie_guide newInstance() {
            return new Newbie_guide();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.content_newbie, container, false);
            TextView targetText = rootView.findViewById(R.id.help_newbie);
            targetText.setText(Html.fromHtml(getString(R.string.help_newbie)));

            Button closeWindow = rootView.findViewById(R.id.close_window);
            closeWindow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActivity().finish();
                }
            });

            return rootView;
        }
    }


    public static class License_fragment extends Fragment {
        TextView licenceTextView;
        ProgressBar progressBar;

        public License_fragment() {
        }

        public static License_fragment newInstance() {
            return new License_fragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.content_license, container, false);

            licenceTextView = rootView.findViewById(R.id.text_GPL);
            progressBar = rootView.findViewById(R.id.progressbar);
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            progressBar.setVisibility(View.VISIBLE);
            licenceTextView.setVisibility(View.GONE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream licenseFile = getResources().openRawResource(R.raw.gpl);
                    Spanned GPLHtml = null;

                    try {
                        GPLHtml = Html.fromHtml(SimpleFunctions.readIt(licenseFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    final Spanned finalGPLHtml = GPLHtml;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            licenceTextView.setText(finalGPLHtml);
                            progressBar.setVisibility(View.GONE);
                            licenceTextView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return About_Us.newInstance();
                case 1:
                    return Newbie_guide.newInstance();
                case 2:
                    return License_fragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.about);
                case 1:
                    return getString(R.string.newbies);
                case 2:
                    return getString(R.string.license);
            }
            return null;
        }
    }
}
