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

package vit01.idecmobile.GUI.ManageDatabase;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.List;

import butterknife.BindString;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import vit01.idecmobile.Core.SimpleFunctions;
import vit01.idecmobile.R;

import static com.mikepenz.google_material_typeface_library.GoogleMaterial.Icon;

public class WhatToManageFragment extends Fragment {
    Activity activity;
    int secondaryText;
    @BindString(R.string.action_config_import)
    String import_str;
    @BindString(R.string.action_config_export)
    String export_str;
    @BindViews({R.id.btn_manage_messages_db, R.id.btn_manage_files_db, R.id.btn_manage_cache})
    List<Button> buttons;
    private Unbinder unbinder;

    public WhatToManageFragment() {
    }

    public static WhatToManageFragment newInstance() {
        WhatToManageFragment fragment = new WhatToManageFragment();
        Bundle args = new Bundle();
        /*args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);*/
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_what_to_manage, container, false);
        unbinder = ButterKnife.bind(this, view);
        activity = getActivity();

        secondaryText = SimpleFunctions.colorFromTheme(activity, android.R.attr.textColorSecondary);
        final int iconSize = 40;
        final GoogleMaterial.Icon icons[] = {Icon.gmd_message, Icon.gmd_folder_shared, Icon.gmd_cached};

        ButterKnife.apply(buttons, new ButterKnife.Action<Button>() {
            @Override
            public void apply(@NonNull Button btn, int index) {
                btn.setCompoundDrawablesWithIntrinsicBounds(null, new IconicsDrawable(activity, icons[index])
                        .sizeDp(iconSize).color(secondaryText), null, null);
                btn.setCompoundDrawablePadding(iconSize);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.btn_manage_back, R.id.btn_manage_messages_db,
            R.id.btn_manage_files_db, R.id.btn_manage_cache})

    public void actionClicker(View button) {
        activity = getActivity();

        switch (button.getId()) {
            case R.id.btn_manage_back:
                activity.finish();
                break;
            case R.id.btn_manage_messages_db:
                Toast.makeText(activity, "Not implemented", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_manage_files_db:
                break;
            case R.id.btn_manage_cache:
                break;
        }
    }
}
