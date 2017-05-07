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

package vit01.idecmobile.bugreports;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import org.acra.dialog.BaseCrashReportDialog;

public class CrashReportActivity extends BaseCrashReportDialog
        implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Упс, страшная ошибка!")
                .setMessage("Если ты очень добрый и хороший человек, отошли нам письмо о том, что произошло")
                .setPositiveButton("Отправить", this)
                .setNegativeButton("Отмена", this)
                .create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            sendCrash("", "");
        } else {
            cancelReports();
        }
        finish();
    }
}