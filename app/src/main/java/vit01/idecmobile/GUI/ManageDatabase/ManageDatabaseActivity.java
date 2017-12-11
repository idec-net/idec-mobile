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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import vit01.idecmobile.R;
import vit01.idecmobile.prefs.Config;

public class ManageDatabaseActivity extends AppCompatActivity {
    // @BindView(R.id.manage_database_layout) CoordinatorLayout rootLayout;

    @Override
    public void onCreate(Bundle savedInstance) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_manage_database);
        ButterKnife.bind(this);

        WhatToManageFragment firstFragment = WhatToManageFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.manage_database_layout, firstFragment).commit();
    }
}