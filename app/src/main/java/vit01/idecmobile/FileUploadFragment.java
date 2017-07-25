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

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.prefs.Config;

public class FileUploadFragment extends Fragment {
    String fecho, file_path;
    int nodeindex;

    TextInputEditText fechoarea, edit_filename, description;
    Spinner stations_spinner;

    public static FileUploadFragment newInstance() {
        return new FileUploadFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.file_upload_fragment, container, false);

        rootView.findViewById(R.id.action_choose_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });

        rootView.findViewById(R.id.action_file_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendFile();
            }
        });

        stations_spinner = (Spinner) rootView.findViewById(R.id.upload_stations);
        fechoarea = (TextInputEditText) rootView.findViewById(R.id.file_echoarea);
        edit_filename = (TextInputEditText) rootView.findViewById(R.id.filename);
        description = (TextInputEditText) rootView.findViewById(R.id.file_description);

        stations_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                nodeindex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        nodeindex = Config.currentSelectedStation;

        SpinnerAdapter spinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
                IDECFunctions.getStationsNames());
        stations_spinner.setAdapter(spinnerAdapter);
        stations_spinner.setSelection(nodeindex);
    }

    public void chooseFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, 12);
    }

    public void sendFile() {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 12) return;

        if (data == null) {
            Toast.makeText(getActivity(), "Nothing to choose", Toast.LENGTH_SHORT).show();
        } else {
            Uri file_uri = data.getData();
            ContentResolver cr = getActivity().getContentResolver();

            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor metaCursor = cr.query(file_uri, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        String filename = metaCursor.getString(0);
                        edit_filename.setText(filename);
                    }
                } finally {
                    metaCursor.close();
                }
            }

            projection = new String[]{MediaStore.MediaColumns.DATA};

            metaCursor = cr.query(file_uri, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        file_path = metaCursor.getString(0);
                    }
                } finally {
                    metaCursor.close();
                }
            }

            Toast.makeText(getActivity(), file_path, Toast.LENGTH_SHORT).show();
        }
    }
}
