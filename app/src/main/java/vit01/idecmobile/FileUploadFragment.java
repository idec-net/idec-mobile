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
import android.provider.OpenableColumns;
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

import java.io.InputStream;

import vit01.idecmobile.Core.IDECFunctions;
import vit01.idecmobile.prefs.Config;

public class FileUploadFragment extends Fragment {
    String fecho = null;
    int nodeindex;

    TextInputEditText fechoarea, edit_filename, description;
    Spinner stations_spinner;

    Uri input = null;
    long filesize = 0;

    public static FileUploadFragment newInstance() {
        return new FileUploadFragment();
    }

    public static FileUploadFragment newInstance(String echo) {
        FileUploadFragment fragm = new FileUploadFragment();
        fragm.fecho = echo;
        return fragm;
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

        if (fecho != null) fechoarea.setText(fecho);
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
        if (input == null || filesize == 0) {
            Toast.makeText(getActivity(), R.string.file_not_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String fecho = fechoarea.getText().toString().toLowerCase().replace(" ", "").trim();
        String fname = edit_filename.getText().toString().toLowerCase().replace(" ", "").trim();
        String desc = description.getText().toString()
                .replace("\r", " ").replace("\n", " ").trim();

        if (fname.equals("") || fecho.equals("") || desc.equals("")) {
            Toast.makeText(getActivity(), R.string.not_all_fields_specified, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent startLoading = new Intent(getActivity(), ProgressActivity.class);
        startLoading.putExtra("task", "upload_fp");
        startLoading.putExtra("nodeindex", nodeindex);
        startLoading.putExtra("filename", fname);
        startLoading.putExtra("filesize", filesize);
        startLoading.putExtra("fecho", fecho);
        startLoading.putExtra("description", desc);
        startLoading.putExtra("inputstream", input);

        startActivity(startLoading);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 12) return;

        if (data != null) {
            Uri file_uri = data.getData();
            ContentResolver cr = getActivity().getContentResolver();

            String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
            Cursor metaCursor = cr.query(file_uri, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        String filename = metaCursor.getString(0);
                        if (filename.contains(":")) filename = filename.replace(":", ".");
                        if (!filename.contains(".")) filename += ".bin";
                        edit_filename.setText(filename);
                    }
                } finally {
                    metaCursor.close();
                }
            }

            long size = 0;

            projection = new String[]{OpenableColumns.SIZE};
            metaCursor = cr.query(file_uri, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        size = metaCursor.getLong(0);
                    }
                } finally {
                    metaCursor.close();
                }
            }

            if (size > 0) {
                try {
                    InputStream is = cr.openInputStream(file_uri);
                    input = file_uri;
                    filesize = size;
                    Toast.makeText(getActivity(),
                            getString(R.string.file_chosen, edit_filename.getText()), Toast.LENGTH_SHORT).show();
                    if (is != null) is.close();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), getString(R.string.error_formatted,
                            e.toString()), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}