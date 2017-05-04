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

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.GlobalTransport;

public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
            if (query.equals("___query_empty")) query = null;

            if (bundle == null) bundle = new Bundle();
            showResults(query, bundle);
        }
    }

    private void showResults(final String query, Bundle bundle) {
        final String subjKey = bundle.getString("subj");
        final List<String> echoareas = bundle.getStringArrayList("echoareas");
        final List<String> senders = bundle.getStringArrayList("senders");
        final List<String> receivers = bundle.getStringArrayList("receivers");
        final List<String> addresses = bundle.getStringArrayList("addresses");
        final Long time1 = (Long) bundle.getSerializable("time1");
        final Long time2 = (Long) bundle.getSerializable("time2");
        final boolean is_favorite = bundle.getBoolean("is_favorite");

        if (query == null && subjKey == null &&
                (echoareas == null || echoareas.size() == 0) &&
                (senders == null || senders.size() == 0) &&
                (receivers == null || receivers.size() == 0) &&
                (addresses == null || addresses.size() == 0) &&
                (time1 == null || time2 == null) &&
                !is_favorite) {

            Toast.makeText(SearchActivity.this, "Введены пустые параметры! Искать нечего", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        final ProgressDialog progress = ProgressDialog.show(SearchActivity.this, "Идёт поиск", "Подождите-ка...", true);
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> msgids = GlobalTransport.transport.searchQuery(
                                query, subjKey, echoareas, senders, receivers, addresses,
                                time1, time2, is_favorite);

                        if (msgids.size() > 0) {
                            Intent intent = new Intent(SearchActivity.this, MessageSlideActivity.class);
                            intent.putExtra("msglist", msgids);
                            intent.putExtra("position", 0);
                            startActivity(intent);
                        } else
                            Toast.makeText(SearchActivity.this, "Ничего не найдено!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        }).start();
    }
}