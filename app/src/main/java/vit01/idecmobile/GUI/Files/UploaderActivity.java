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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class UploaderActivity extends AppCompatActivity {
    String fecho = null;
    Uri extrauri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploader);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SimpleFunctions.setDisplayHomeAsUpEnabled(this);
        SimpleFunctions.setActivityTitle(this, getString(R.string.action_file_upload));

        // Обработка операций в фэхах
        Intent gotIntent = getIntent();
        if (gotIntent.hasExtra("fecho")) {
            fecho = gotIntent.getStringExtra("fecho");
        } else if (Intent.ACTION_SEND.equals(gotIntent.getAction())) {
            Uri fileUri = gotIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) extrauri = fileUri;
        }

        FileUploadFragment fragm = FileUploadFragment.newInstance(fecho, extrauri);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragm).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode > 3) return;

        if (resultCode == RESULT_OK) {
            final File file = (File) data.getSerializableExtra("selected_file");
            Toast.makeText(UploaderActivity.this, getString(R.string.file_chosen, file.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        }
    }
}
